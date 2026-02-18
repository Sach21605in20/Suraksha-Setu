package com.orthowatch.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
@Table(name = "recovery_templates")
@EntityListeners(AuditingEntityListener.class)
public class RecoveryTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "surgery_type", unique = true, nullable = false)
  private String surgeryType;

  @NotNull
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @NotNull
  @Type(JsonType.class)
  @Column(name = "checklist_config", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> checklistConfig;

  @NotNull
  @Type(JsonType.class)
  @Column(name = "milestone_config", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> milestoneConfig;

  @Builder.Default
  @Column(name = "mandatory_image_days", columnDefinition = "integer[]", nullable = false)
  private List<Integer> mandatoryImageDays = List.of(3, 5);

  @Builder.Default
  @ColumnDefault("14")
  @Column(name = "monitoring_days", nullable = false)
  private int monitoringDays = 14;

  @Builder.Default
  @ColumnDefault("true")
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

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
