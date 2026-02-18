package com.orthowatch.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_scores")
@EntityListeners(AuditingEntityListener.class)
public class RiskScore {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id", nullable = false)
  private Episode episode;

  @Column(name = "day_number", nullable = false)
  private int dayNumber;

  @Column(name = "composite_score", nullable = false)
  private int compositeScore;

  @Column(name = "risk_level", nullable = false)
  private String riskLevel; // LOW, MEDIUM, HIGH

  @Type(JsonType.class)
  @Column(name = "contributing_factors", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> contributingFactors;

  private String trajectory; // IMPROVING, STABLE, WORSENING

  @Column(name = "rule_version_id", nullable = false)
  private String ruleVersionId;

  @Type(JsonType.class)
  @Column(name = "rule_set_snapshot", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> ruleSetSnapshot;

  @Builder.Default
  @Column(name = "calculated_at", nullable = false)
  private OffsetDateTime calculatedAt = OffsetDateTime.now();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
