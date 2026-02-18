package com.orthowatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "episodes")
@EntityListeners(AuditingEntityListener.class)
public class Episode {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private RecoveryTemplate template;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "primary_surgeon_id", nullable = false)
  private User primarySurgeon;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "secondary_clinician_id")
  private User secondaryClinician;

  @NotNull
  @Column(name = "surgery_date", nullable = false)
  private LocalDate surgeryDate;

  @NotNull
  @Column(name = "discharge_date", nullable = false)
  private LocalDate dischargeDate;

  @Builder.Default
  @ColumnDefault("0")
  @Column(name = "current_day", nullable = false)
  private int currentDay = 0;

  @Builder.Default
  @ColumnDefault("'ACTIVE'")
  @Column(nullable = false)
  private String status = "ACTIVE"; // ACTIVE, COMPLETED, PAUSED, CANCELLED

  @Column(name = "pain_score_discharge", nullable = false)
  private int painScoreDischarge;

  @Column(name = "swelling_level_discharge", nullable = false)
  private String swellingLevelDischarge; // NONE, MILD, MODERATE, SEVERE

  @Builder.Default
  @ColumnDefault("'PENDING'")
  @Column(name = "consent_status", nullable = false)
  private String consentStatus = "PENDING"; // PENDING, GRANTED, DECLINED

  @Column(name = "consent_timestamp")
  private OffsetDateTime consentTimestamp;

  @Builder.Default
  @ColumnDefault("'09:00'")
  @Column(name = "checklist_time", nullable = false)
  private LocalTime checklistTime = LocalTime.of(9, 0);

  @Builder.Default
  @ColumnDefault("'Asia/Kolkata'")
  @Column(nullable = false)
  private String timezone = "Asia/Kolkata";

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
