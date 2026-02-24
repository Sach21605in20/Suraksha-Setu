package com.orthowatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentRequest {

  @NotBlank(message = "Patient name is required")
  @Size(min = 2, max = 255, message = "Patient name must be between 2 and 255 characters")
  private String patientName;

  @NotNull(message = "Age is required")
  @Min(value = 1, message = "Age must be at least 1")
  @Max(value = 149, message = "Age must be less than 150")
  private Integer age;

  private String gender;

  @NotBlank(message = "Primary phone number is required")
  @Pattern(
      regexp = "^\\+91[6-9]\\d{9}$",
      message = "Phone must be a valid Indian mobile number (e.g., +919876543210)")
  private String phonePrimary;

  @Pattern(
      regexp = "^\\+91[6-9]\\d{9}$",
      message = "Caregiver phone must be a valid Indian mobile number")
  private String phoneCaregiver;

  @Builder.Default private String preferredLanguage = "en";

  private String hospitalMrn;

  @NotBlank(message = "Surgery type is required")
  private String surgeryType;

  @NotNull(message = "Surgery date is required")
  private LocalDate surgeryDate;

  @NotNull(message = "Discharge date is required")
  private LocalDate dischargeDate;

  @NotNull(message = "Primary surgeon ID is required")
  private UUID primarySurgeonId;

  private UUID secondaryClinicianId;

  @NotNull(message = "Pain score at discharge is required")
  @Min(value = 0, message = "Pain score must be between 0 and 10")
  @Max(value = 10, message = "Pain score must be between 0 and 10")
  private Integer painScoreDischarge;

  @NotBlank(message = "Swelling level at discharge is required")
  @Pattern(
      regexp = "^(NONE|MILD|MODERATE|SEVERE)$",
      message = "Swelling level must be one of: NONE, MILD, MODERATE, SEVERE")
  private String swellingLevelDischarge;
}
