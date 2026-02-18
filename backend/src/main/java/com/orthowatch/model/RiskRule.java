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
@Table(name = "risk_rules")
@EntityListeners(AuditingEntityListener.class)
public class RiskRule {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "rule_name", unique = true, nullable = false)
  private String ruleName;

  @NotNull
  @Column(name = "condition_expression", nullable = false)
  private String conditionExpression;

  @NotNull
  @Column(name = "risk_level", nullable = false)
  private String riskLevel; // LOW, MEDIUM, HIGH

  @Column(nullable = false)
  private int weight;

  @Builder.Default
  @ColumnDefault("true")
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Builder.Default
  @ColumnDefault("1")
  @Column(nullable = false)
  private int version = 1;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
