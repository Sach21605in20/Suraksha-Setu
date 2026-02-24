package com.orthowatch.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.dto.EnrollmentResponse;
import com.orthowatch.exception.DuplicateResourceException;
import com.orthowatch.exception.ResourceNotFoundException;
import com.orthowatch.mapper.EpisodeMapper;
import com.orthowatch.mapper.PatientMapper;
import com.orthowatch.model.ClinicalAuditLog;
import com.orthowatch.model.ConsentLog;
import com.orthowatch.model.Episode;
import com.orthowatch.model.Patient;
import com.orthowatch.model.RecoveryTemplate;
import com.orthowatch.model.User;
import com.orthowatch.repository.ClinicalAuditLogRepository;
import com.orthowatch.repository.ConsentLogRepository;
import com.orthowatch.repository.EpisodeRepository;
import com.orthowatch.repository.PatientRepository;
import com.orthowatch.repository.RecoveryTemplateRepository;
import com.orthowatch.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

  @Mock private PatientRepository patientRepository;
  @Mock private EpisodeRepository episodeRepository;
  @Mock private RecoveryTemplateRepository recoveryTemplateRepository;
  @Mock private UserRepository userRepository;
  @Mock private ConsentLogRepository consentLogRepository;
  @Mock private ClinicalAuditLogRepository clinicalAuditLogRepository;
  @Mock private PatientMapper patientMapper;
  @Mock private EpisodeMapper episodeMapper;
  @Mock private Scheduler scheduler;

  @InjectMocks private EnrollmentService enrollmentService;

  private EnrollmentRequest validRequest;
  private User surgeon;
  private User adminUser;
  private RecoveryTemplate template;
  private Patient patient;
  private Episode episode;

  @BeforeEach
  void setUp() {
    UUID surgeonId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID episodeId = UUID.randomUUID();

    surgeon =
        User.builder()
            .id(surgeonId)
            .email("surgeon@orthowatch.com")
            .fullName("Dr. Ramesh Kumar")
            .role("SURGEON")
            .isActive(true)
            .build();

    adminUser =
        User.builder()
            .id(UUID.randomUUID())
            .email("admin@orthowatch.com")
            .fullName("Admin User")
            .role("ADMIN")
            .isActive(true)
            .build();

    template =
        RecoveryTemplate.builder()
            .id(templateId)
            .surgeryType("TKR")
            .displayName("Total Knee Replacement")
            .isActive(true)
            .build();

    patient =
        Patient.builder()
            .id(patientId)
            .fullName("Lakshmi Devi")
            .age(64)
            .gender("F")
            .phonePrimary("+919876543210")
            .preferredLanguage("hi")
            .build();

    episode =
        Episode.builder()
            .id(episodeId)
            .patient(patient)
            .template(template)
            .primarySurgeon(surgeon)
            .surgeryDate(LocalDate.of(2026, 2, 13))
            .dischargeDate(LocalDate.of(2026, 2, 15))
            .painScoreDischarge(6)
            .swellingLevelDischarge("MODERATE")
            .status("ACTIVE")
            .consentStatus("PENDING")
            .build();

    validRequest =
        EnrollmentRequest.builder()
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
  @DisplayName("Should enroll new patient successfully — happy path")
  void shouldEnrollNewPatientSuccessfully() throws Exception {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(surgeon));
    when(patientRepository.findByPhonePrimary("+919876543210")).thenReturn(Optional.empty());
    when(patientMapper.toEntity(validRequest)).thenReturn(patient);
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);
    when(episodeMapper.toEntity(validRequest)).thenReturn(episode);
    when(episodeRepository.save(any(Episode.class))).thenReturn(episode);
    when(consentLogRepository.save(any(ConsentLog.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(clinicalAuditLogRepository.save(any(ClinicalAuditLog.class)))
        .thenAnswer(i -> i.getArgument(0));

    // When
    EnrollmentResponse response =
        enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getEpisodeId()).isEqualTo(episode.getId());
    assertThat(response.getPatientId()).isEqualTo(patient.getId());
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
    assertThat(response.getConsentStatus()).isEqualTo("PENDING");
    assertThat(response.getMessage()).contains("Patient enrolled");

    // Verify all repositories were called
    verify(patientRepository).save(any(Patient.class));
    verify(episodeRepository).save(any(Episode.class));
    verify(consentLogRepository).save(any(ConsentLog.class));
    verify(clinicalAuditLogRepository).save(any(ClinicalAuditLog.class));
    verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }

  @Test
  @DisplayName("Should reuse existing patient by phone — creates new episode only")
  void shouldReuseExistingPatientByPhone() throws Exception {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(surgeon));
    when(patientRepository.findByPhonePrimary("+919876543210"))
        .thenReturn(Optional.of(patient));
    when(episodeRepository.existsByPatientIdAndTemplateSurgeryTypeAndStatus(
            patient.getId(), "TKR", "ACTIVE"))
        .thenReturn(false);
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);
    when(episodeMapper.toEntity(validRequest)).thenReturn(episode);
    when(episodeRepository.save(any(Episode.class))).thenReturn(episode);
    when(consentLogRepository.save(any(ConsentLog.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(clinicalAuditLogRepository.save(any(ClinicalAuditLog.class)))
        .thenAnswer(i -> i.getArgument(0));

    // When
    EnrollmentResponse response =
        enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getPatientId()).isEqualTo(patient.getId());

    // Verify patientMapper.updateEntity was called (reuse path)
    verify(patientMapper).updateEntity(eq(validRequest), eq(patient));
    // Verify patientMapper.toEntity was NOT called (new patient path)
    verify(patientMapper, never()).toEntity(any());
  }

  @Test
  @DisplayName("Should return 404 when surgeon not found")
  void shouldReturn404WhenSurgeonNotFound() {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.empty());

    // When + Then
    assertThatThrownBy(
            () -> enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Surgeon not found");
  }

  @Test
  @DisplayName("Should return 404 when surgeon has wrong role")
  void shouldReturn404WhenSurgeonHasWrongRole() {
    // Given
    User nurse =
        User.builder()
            .id(validRequest.getPrimarySurgeonId())
            .email("nurse@orthowatch.com")
            .role("NURSE")
            .build();
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(nurse));

    // When + Then
    assertThatThrownBy(
            () -> enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Surgeon not found");
  }

  @Test
  @DisplayName("Should return 404 when template not found")
  void shouldReturn404WhenTemplateNotFound() {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.empty());

    // When + Then
    assertThatThrownBy(
            () -> enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("No active recovery template found");
  }

  @Test
  @DisplayName("Should return 409 when duplicate active episode exists")
  void shouldReturn409WhenDuplicateActiveEpisode() {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(surgeon));
    when(patientRepository.findByPhonePrimary("+919876543210"))
        .thenReturn(Optional.of(patient));
    when(episodeRepository.existsByPatientIdAndTemplateSurgeryTypeAndStatus(
            patient.getId(), "TKR", "ACTIVE"))
        .thenReturn(true);

    // When + Then
    assertThatThrownBy(
            () -> enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("already has an active episode");
  }

  @Test
  @DisplayName("Should create consent log with correct fields")
  void shouldCreateConsentLogWithCorrectFields() throws Exception {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(surgeon));
    when(patientRepository.findByPhonePrimary("+919876543210")).thenReturn(Optional.empty());
    when(patientMapper.toEntity(validRequest)).thenReturn(patient);
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);
    when(episodeMapper.toEntity(validRequest)).thenReturn(episode);
    when(episodeRepository.save(any(Episode.class))).thenReturn(episode);
    when(consentLogRepository.save(any(ConsentLog.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(clinicalAuditLogRepository.save(any(ClinicalAuditLog.class)))
        .thenAnswer(i -> i.getArgument(0));

    // When
    enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent");

    // Then
    ArgumentCaptor<ConsentLog> consentCaptor = ArgumentCaptor.forClass(ConsentLog.class);
    verify(consentLogRepository).save(consentCaptor.capture());
    ConsentLog savedConsent = consentCaptor.getValue();

    assertThat(savedConsent.getConsentType()).isEqualTo("MONITORING");
    assertThat(savedConsent.getMethod()).isEqualTo("WHATSAPP");
    assertThat(savedConsent.getConsentText()).contains("OrthoWatch Recovery Monitoring");
    assertThat(savedConsent.getPatient()).isEqualTo(patient);
    assertThat(savedConsent.getEpisode()).isEqualTo(episode);
  }

  @Test
  @DisplayName("Should schedule Quartz consent timeout job")
  void shouldScheduleQuartzConsentTimeoutJob() throws Exception {
    // Given
    when(recoveryTemplateRepository.findBySurgeryTypeAndIsActiveTrue("TKR"))
        .thenReturn(Optional.of(template));
    when(userRepository.findById(validRequest.getPrimarySurgeonId()))
        .thenReturn(Optional.of(surgeon));
    when(patientRepository.findByPhonePrimary("+919876543210")).thenReturn(Optional.empty());
    when(patientMapper.toEntity(validRequest)).thenReturn(patient);
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);
    when(episodeMapper.toEntity(validRequest)).thenReturn(episode);
    when(episodeRepository.save(any(Episode.class))).thenReturn(episode);
    when(consentLogRepository.save(any(ConsentLog.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(clinicalAuditLogRepository.save(any(ClinicalAuditLog.class)))
        .thenAnswer(i -> i.getArgument(0));

    // When
    enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent");

    // Then
    ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());

    JobDetail job = jobCaptor.getValue();
    assertThat(job.getJobDataMap().getString("episodeId")).isEqualTo(episode.getId().toString());
    assertThat(job.getKey().getName()).startsWith("consent-timeout-");
    assertThat(job.getKey().getGroup()).isEqualTo("enrollment");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when surgeryDate is after dischargeDate")
  void shouldThrowWhenSurgeryDateAfterDischargeDate() {
    // Given
    validRequest.setSurgeryDate(LocalDate.of(2026, 2, 20));
    validRequest.setDischargeDate(LocalDate.of(2026, 2, 15));

    // When + Then
    assertThatThrownBy(
            () -> enrollmentService.enroll(validRequest, adminUser, "127.0.0.1", "Test-Agent"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Surgery date must be on or before discharge date");
  }
}
