package com.orthowatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "consent_logs")
@EntityListeners(AuditingEntityListener.class)
public class ConsentLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id", nullable = false)
  private Episode episode;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Column(name = "consent_type", nullable = false)
  private String consentType;

  @Column(nullable = false)
  private String status; // GRANTED, DECLINED, REVOKED

  @Column(nullable = false)
  private String method; // WHATSAPP, MANUAL, VERBAL

  @Column(name = "consent_text", nullable = false)
  private String consentText;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "granted_at")
  private OffsetDateTime grantedAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
