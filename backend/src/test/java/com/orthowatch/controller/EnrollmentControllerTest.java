package com.orthowatch.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.dto.EnrollmentResponse;
import com.orthowatch.exception.GlobalExceptionHandler;
import com.orthowatch.model.User;
import com.orthowatch.repository.UserRepository;
import com.orthowatch.service.EnrollmentService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller slice tests for {@link EnrollmentController}.
 *
 * <p>Uses {@code @ContextConfiguration} to load only the controller and exception handler,
 * bypassing the main application class which has {@code @EnableJpaRepositories} that triggers JPA.
 * Security filters remain active so {@code @WithMockUser} can inject {@code Authentication}.
 */
@WebMvcTest
@ContextConfiguration(classes = {EnrollmentController.class, GlobalExceptionHandler.class})
class EnrollmentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private EnrollmentService enrollmentService;
  @MockBean private UserRepository userRepository;

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
        .primarySurgeonId(UUID.randomUUID())
        .painScoreDischarge(6)
        .swellingLevelDischarge("MODERATE")
        .build();
  }

  @Test
  @DisplayName("Should return 201 on successful enrollment")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn201OnSuccessfulEnrollment() throws Exception {
    EnrollmentRequest request = createValidRequest();
    UUID episodeId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    EnrollmentResponse response =
        EnrollmentResponse.builder()
            .episodeId(episodeId)
            .patientId(patientId)
            .status("ACTIVE")
            .consentStatus("PENDING")
            .message("Patient enrolled. Consent message sent via WhatsApp.")
            .build();

    User adminUser =
        User.builder()
            .id(UUID.randomUUID())
            .email("admin@orthowatch.com")
            .role("ADMIN")
            .build();

    when(userRepository.findByEmail("admin@orthowatch.com")).thenReturn(Optional.of(adminUser));
    when(enrollmentService.enroll(
            any(EnrollmentRequest.class), any(User.class), any(), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.episodeId").value(episodeId.toString()))
        .andExpect(jsonPath("$.patientId").value(patientId.toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.consentStatus").value("PENDING"))
        .andExpect(
            jsonPath("$.message")
                .value("Patient enrolled. Consent message sent via WhatsApp."));
  }

  @Test
  @DisplayName("Should return 400 when patient name is missing")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenPatientNameMissing() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setPatientName(null);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when patient name is too short")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenPatientNameTooShort() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setPatientName("A");

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when phone format is invalid")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenPhoneFormatInvalid() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setPhonePrimary("1234567890");

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when pain score is out of range")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenPainScoreOutOfRange() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setPainScoreDischarge(15);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when swelling level is invalid")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenSwellingLevelInvalid() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setSwellingLevelDischarge("EXTREME");

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when surgery type is missing")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenSurgeryTypeMissing() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setSurgeryType(null);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when primary surgeon ID is missing")
  @WithMockUser(username = "admin@orthowatch.com", roles = "ADMIN")
  void shouldReturn400WhenPrimarySurgeonIdMissing() throws Exception {
    EnrollmentRequest request = createValidRequest();
    request.setPrimarySurgeonId(null);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
