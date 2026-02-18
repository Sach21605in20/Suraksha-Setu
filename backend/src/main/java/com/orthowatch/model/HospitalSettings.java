package com.orthowatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "hospital_settings")
@EntityListeners(AuditingEntityListener.class)
public class HospitalSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "hospital_name", nullable = false)
  private String hospitalName;

  @Builder.Default
  @ColumnDefault("'Asia/Kolkata'")
  @Column(nullable = false)
  private String timezone = "Asia/Kolkata";

  @Builder.Default
  @ColumnDefault("'09:00'")
  @Column(name = "default_checklist_time", nullable = false)
  private LocalTime defaultChecklistTime = LocalTime.of(9, 0);

  @Column(name = "emergency_phone")
  private String emergencyPhone;

  @Builder.Default
  @ColumnDefault("5")
  @Column(name = "dashboard_refresh_interval_min", nullable = false)
  private int dashboardRefreshIntervalMin = 5;

  @Builder.Default
  @ColumnDefault("2")
  @Column(name = "escalation_sla_hours", nullable = false)
  private int escalationSlaHours = 2;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
