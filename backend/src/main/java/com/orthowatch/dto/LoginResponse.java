package com.orthowatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String refreshToken;
    private Long id;
    private String email;
    private String fullName;
    private String role;
}
