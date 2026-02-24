package com.orthowatch.controller;

import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.dto.EnrollmentResponse;
import com.orthowatch.model.User;
import com.orthowatch.repository.UserRepository;
import com.orthowatch.service.EnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

  private final EnrollmentService enrollmentService;
  private final UserRepository userRepository;

  @PostMapping
  @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
  public ResponseEntity<EnrollmentResponse> enroll(
      @Valid @RequestBody EnrollmentRequest request,
      Authentication authentication,
      HttpServletRequest httpRequest) {

    String email = authentication.getName();
    User currentUser =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");

    EnrollmentResponse response =
        enrollmentService.enroll(request, currentUser, ipAddress, userAgent);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
