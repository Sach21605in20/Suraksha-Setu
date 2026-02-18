package com.orthowatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wound_images")
@EntityListeners(AuditingEntityListener.class)
public class WoundImage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id", nullable = false)
  private Episode episode;

  @Column(name = "day_number", nullable = false)
  private int dayNumber;

  @Column(name = "storage_path", nullable = false)
  private String storagePath;

  @Column(name = "storage_provider", nullable = false)
  private String storageProvider; // LOCAL, SUPABASE, CLOUDFLARE_R2

  @Column(name = "file_size_bytes", nullable = false)
  private long fileSizeBytes;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "is_mandatory", nullable = false)
  private boolean isMandatory;

  @Builder.Default
  @ColumnDefault("true")
  @Column(nullable = false)
  private boolean encrypted = true;

  @Column(name = "retention_expires_at", nullable = false)
  private OffsetDateTime retentionExpiresAt;

  @Column(name = "uploaded_by", nullable = false)
  private String uploadedBy; // PATIENT, CAREGIVER

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
