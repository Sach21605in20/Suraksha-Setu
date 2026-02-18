package com.orthowatch.model;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_responses")
@EntityListeners(AuditingEntityListener.class)
public class DailyResponse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id", nullable = false)
  private Episode episode;

  @Column(name = "day_number", nullable = false)
  private int dayNumber;

  @Column(name = "responder_type", nullable = false)
  private String responderType; // PATIENT, CAREGIVER

  @Column(name = "pain_score")
  private Integer painScore;

  @Column(name = "swelling_level")
  private String swellingLevel;

  @Column(name = "fever_level")
  private String feverLevel;

  @Type(ListArrayType.class)
  @Column(name = "dvt_symptoms", columnDefinition = "varchar[]")
  private List<String> dvtSymptoms;

  @Column(name = "mobility_achieved")
  private Boolean mobilityAchieved;

  @Column(name = "medication_adherence")
  private String medicationAdherence;

  @Builder.Default
  @ColumnDefault("'PENDING'")
  @Column(name = "completion_status", nullable = false)
  private String completionStatus = "PENDING"; // PENDING, PARTIAL, COMPLETED

  @Builder.Default
  @ColumnDefault("false")
  @Column(name = "emergency_override", nullable = false)
  private boolean emergencyOverride = false;

  @Column(name = "response_started_at")
  private OffsetDateTime responseStartedAt;

  @Column(name = "response_completed_at")
  private OffsetDateTime responseCompletedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
