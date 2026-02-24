package com.orthowatch.integration;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.dto.EnrollmentResponse;
import com.orthowatch.model.User;
import com.orthowatch.repository.ClinicalAuditLogRepository;
import com.orthowatch.repository.ConsentLogRepository;
import com.orthowatch.repository.EpisodeRepository;
import com.orthowatch.repository.PatientRepository;
import com.orthowatch.repository.RecoveryTemplateRepository;
import com.orthowatch.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EnrollmentIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("orthowatch_test")
          .withUsername("test_user")
          .withPassword("test_pass");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.data.redis.host", () -> "localhost");
    registry.add("spring.data.redis.port", () -> "6379");
    registry.add("app.jwt.secret", () -> "ZGV2c2VjcmV0a2V5bWluaW11bTMyY2hhcmFjdGVyc2xvbmc=");
    registry.add(
        "app.jwt.refresh-secret", () -> "ZGV2cmVmcmVzaHNlY3JldG1pbmltdW0zMmNoYXJhY3RlcnM=");
    registry.add("app.jwt.expiration", () -> "1800000");
    registry.add("app.jwt.refresh-expiration", () -> "604800000");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PatientRepository patientRepository;
  @Autowired private EpisodeRepository episodeRepository;
  @Autowired private ConsentLogRepository consentLogRepository;
  @Autowired private ClinicalAuditLogRepository clinicalAuditLogRepository;
  @Autowired private RecoveryTemplateRepository recoveryTemplateRepository;

  private UUID surgeonId;

  @BeforeEach
  void setUp() {
    // Surgeon is seeded by V4 Flyway migration
    User surgeon = userRepository.findByEmail("surgeon@orthowatch.com").orElse(null);
    if (surgeon != null) {
      surgeonId = surgeon.getId();
    }
  }

  private EnrollmentRequest createValidRequest() {
    return EnrollmentRequest.builder()
        .patientName("Lakshmi Devi")
        .age(64)
        .gender("F")
        .phonePrimary("+919876543210")
        .phoneCaregiver("+919876543211")
        .preferredLanguage("hi")
        .hospitalMrn("MRN-2026-001")
        .surgeryType("TKR")
        .surgeryDate(LocalDate.of(2026, 2, 13))
        .dischargeDate(LocalDate.of(2026, 2, 15))
        .primarySurgeonId(surgeonId)
        .painScoreDischarge(6)
        .swellingLevelDischarge("MODERATE")
        .build();
  }

  @Test
  @DisplayName("Should enroll patient end-to-end — creates patient, episode, consent log, audit log")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldEnrollPatientEndToEnd() throws Exception {
    // Clear any previous test data
    episodeRepository.deleteAll();
    patientRepository.deleteAll();

    EnrollmentRequest request = createValidRequest();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.episodeId").exists())
            .andExpect(jsonPath("$.patientId").exists())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.consentStatus").value("PENDING"))
            .andExpect(jsonPath("$.message").value("Patient enrolled. Consent message sent via WhatsApp."))
            .andReturn();

    EnrollmentResponse response =
        objectMapper.readValue(result.getResponse().getContentAsString(), EnrollmentResponse.class);

    // Verify patient was created in DB
    assertThat(patientRepository.findById(response.getPatientId())).isPresent();
    assertThat(patientRepository.findById(response.getPatientId()).get().getFullName())
        .isEqualTo("Lakshmi Devi");

    // Verify episode was created in DB
    assertThat(episodeRepository.findById(response.getEpisodeId())).isPresent();

    // Verify consent log was created
    assertThat(consentLogRepository.findByEpisodeId(response.getEpisodeId())).isNotEmpty();

    // Verify audit log was created
    assertThat(clinicalAuditLogRepository.findByEpisodeId(response.getEpisodeId())).isNotEmpty();
  }

  @Test
  @DisplayName("Should reject duplicate enrollment — same patient, same surgery type, active episode")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldRejectDuplicateEnrollment() throws Exception {
    // Clear any previous test data
    episodeRepository.deleteAll();
    patientRepository.deleteAll();

    EnrollmentRequest request = createValidRequest();
    // Use a unique phone number for this test to avoid cross-test interference
    request.setPhonePrimary("+919876543299");

    // First enrollment — should succeed
    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Second enrollment with same phone + same surgery type — should fail with 409
    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already has an active episode")));
  }
}
