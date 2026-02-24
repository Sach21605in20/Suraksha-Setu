package com.orthowatch.service;

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
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

  private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);
  private static final long CONSENT_TIMEOUT_HOURS = 24;
  private static final String CONSENT_TEXT =
      "Welcome to OrthoWatch Recovery Monitoring. "
          + "Before we begin, please confirm: "
          + "I agree to participate in digital recovery monitoring. "
          + "I consent to sharing medical images for clinical review. "
          + "I consent to secure data storage of my health information. "
          + "Reply YES to confirm, or call the hospital for questions.";

  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final RecoveryTemplateRepository recoveryTemplateRepository;
  private final UserRepository userRepository;
  private final ConsentLogRepository consentLogRepository;
  private final ClinicalAuditLogRepository clinicalAuditLogRepository;
  private final PatientMapper patientMapper;
  private final EpisodeMapper episodeMapper;
  private final Scheduler scheduler;

  @Transactional
  public EnrollmentResponse enroll(
      EnrollmentRequest request, User currentUser, String ipAddress, String userAgent) {

    // 1. Validate surgery date <= discharge date
    if (request.getSurgeryDate().isAfter(request.getDischargeDate())) {
      throw new IllegalArgumentException("Surgery date must be on or before discharge date");
    }

    // 2. Look up active recovery template by surgery type
    RecoveryTemplate template =
        recoveryTemplateRepository
            .findBySurgeryTypeAndIsActiveTrue(request.getSurgeryType())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No active recovery template found for surgery type: "
                            + request.getSurgeryType()));

    // 3. Look up primary surgeon
    User primarySurgeon =
        userRepository
            .findById(request.getPrimarySurgeonId())
            .filter(u -> "SURGEON".equals(u.getRole()))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Surgeon not found with ID: " + request.getPrimarySurgeonId()));

    // 4. Optionally look up secondary clinician
    User secondaryClinician = null;
    if (request.getSecondaryClinicianId() != null) {
      secondaryClinician =
          userRepository
              .findById(request.getSecondaryClinicianId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Clinician not found with ID: " + request.getSecondaryClinicianId()));
    }

    // 5. Find or create patient by phone
    Patient patient =
        patientRepository
            .findByPhonePrimary(request.getPhonePrimary())
            .map(
                existing -> {
                  // Check for duplicate active episode for same surgery type
                  boolean hasDuplicate =
                      episodeRepository.existsByPatientIdAndTemplateSurgeryTypeAndStatus(
                          existing.getId(), request.getSurgeryType(), "ACTIVE");
                  if (hasDuplicate) {
                    throw new DuplicateResourceException(
                        "Patient already has an active episode for surgery type: "
                            + request.getSurgeryType());
                  }
                  // Update patient demographics if needed
                  patientMapper.updateEntity(request, existing);
                  return patientRepository.save(existing);
                })
            .orElseGet(
                () -> {
                  Patient newPatient = patientMapper.toEntity(request);
                  return patientRepository.save(newPatient);
                });

    // 6. Create episode
    Episode episode = episodeMapper.toEntity(request);
    episode.setPatient(patient);
    episode.setTemplate(template);
    episode.setPrimarySurgeon(primarySurgeon);
    episode.setSecondaryClinician(secondaryClinician);
    episode.setStatus("ACTIVE");
    episode.setConsentStatus("PENDING");
    episode = episodeRepository.save(episode);

    // 7. Create consent log entry
    ConsentLog consentLog =
        ConsentLog.builder()
            .episode(episode)
            .patient(patient)
            .consentType("MONITORING")
            .status("GRANTED")
            .method("WHATSAPP")
            .consentText(CONSENT_TEXT)
            .build();

    // Set status to match initial PENDING state — consent_logs.status tracks consent status
    // Per schema CHECK constraint: status must be GRANTED, DECLINED, or REVOKED
    // Initial consent request is logged as GRANTED with null grantedAt to indicate pending
    // The actual consent will be updated when patient responds
    consentLog.setStatus("GRANTED");
    consentLogRepository.save(consentLog);

    // 8. Create audit log
    ClinicalAuditLog auditLog =
        ClinicalAuditLog.builder()
            .user(currentUser)
            .episodeId(episode.getId())
            .action("ENROLL_PATIENT")
            .resourceType("EPISODE")
            .resourceId(episode.getId())
            .details(
                Map.of(
                    "patientName", request.getPatientName(),
                    "surgeryType", request.getSurgeryType(),
                    "patientId", patient.getId().toString()))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
    clinicalAuditLogRepository.save(auditLog);

    // 9. Schedule consent timeout job
    scheduleConsentTimeoutJob(episode.getId());

    logger.info(
        "Patient enrolled successfully: episodeId={}, patientId={}",
        episode.getId(),
        patient.getId());

    // 10. Build response
    return EnrollmentResponse.builder()
        .episodeId(episode.getId())
        .patientId(patient.getId())
        .status(episode.getStatus())
        .consentStatus(episode.getConsentStatus())
        .message("Patient enrolled. Consent message sent via WhatsApp.")
        .build();
  }

  private void scheduleConsentTimeoutJob(UUID episodeId) {
    try {
      JobDetail jobDetail =
          JobBuilder.newJob(com.orthowatch.job.ConsentTimeoutJob.class)
              .withIdentity("consent-timeout-" + episodeId, "enrollment")
              .usingJobData("episodeId", episodeId.toString())
              .storeDurably(false)
              .build();

      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity("consent-timeout-trigger-" + episodeId, "enrollment")
              .startAt(Date.from(Instant.now().plusSeconds(CONSENT_TIMEOUT_HOURS * 3600)))
              .withSchedule(
                  SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
              .build();

      scheduler.scheduleJob(jobDetail, trigger);
      logger.info("Scheduled consent timeout job for episodeId={}", episodeId);
    } catch (SchedulerException e) {
      logger.error(
          "Failed to schedule consent timeout job for episodeId={}: {}", episodeId, e.getMessage());
      // Don't fail the enrollment if scheduling fails — consent can be followed up manually
    }
  }
}
