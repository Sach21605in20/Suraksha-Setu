package com.orthowatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
public class Alert {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id", nullable = false)
  private Episode episode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "risk_score_id")
  private RiskScore riskScore;

  @Column(name = "alert_type", nullable = false)
  private String alertType; // HIGH_RISK, NON_RESPONSE, EMERGENCY_OVERRIDE, CONSENT_TIMEOUT

  @Column(nullable = false)
  private String severity; // LOW, MEDIUM, HIGH, CRITICAL

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to", nullable = false)
  private User assignedTo;

  @Builder.Default
  @ColumnDefault("'PENDING'")
  @Column(nullable = false)
  private String status = "PENDING"; // PENDING, ACKNOWLEDGED, RESOLVED, EXPIRED, CANCELLED

  @Column(name = "acknowledged_at")
  private OffsetDateTime acknowledgedAt;

  @Column(name = "resolved_at")
  private OffsetDateTime resolvedAt;

  @Column(name = "escalation_outcome")
  private String escalationOutcome;

  @Column(name = "escalation_notes")
  private String escalationNotes;

  @Builder.Default
  @ColumnDefault("false")
  @Column(name = "auto_forwarded", nullable = false)
  private boolean autoForwarded = false;

  @Column(name = "sla_deadline")
  private OffsetDateTime slaDeadline;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
