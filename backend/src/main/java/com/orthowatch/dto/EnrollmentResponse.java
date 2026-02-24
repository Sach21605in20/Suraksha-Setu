package com.orthowatch.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentResponse {
  private UUID episodeId;
  private UUID patientId;
  private String status;
  private String consentStatus;
  private String message;
}
