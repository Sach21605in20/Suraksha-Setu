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
@Table(name = "patients")
@EntityListeners(AuditingEntityListener.class)
public class Patient {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "full_name", nullable = false)
  private String fullName;

  @NotNull
  @Column(nullable = false)
  private int age;

  private String gender;

  @NotNull
  @Column(name = "phone_primary", nullable = false)
  private String phonePrimary;

  @Column(name = "phone_caregiver")
  private String phoneCaregiver;

  @Builder.Default
  @ColumnDefault("'en'")
  @Column(name = "preferred_language", nullable = false)
  private String preferredLanguage = "en";

  @Column(name = "hospital_mrn")
  private String hospitalMrn;

  @Column(name = "fhir_patient_id")
  private String fhirPatientId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
