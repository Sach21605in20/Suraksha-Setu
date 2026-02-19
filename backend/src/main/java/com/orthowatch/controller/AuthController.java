package com.orthowatch.controller;

import com.orthowatch.dto.LoginRequest;
import com.orthowatch.dto.LoginResponse;
import com.orthowatch.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request);
        
        // Set refresh token in HTTP-only cookie
        Cookie refreshCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set to true in production (requires HTTPS)
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (refreshExpiration / 1000));
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        LoginResponse loginResponse = authService.refreshToken(refreshToken);
        
        // Update refresh token cookie if rotated (optional, but good practice if rotation is implemented)
        // LoginResponse returns the same refresh token in current implementation, but we set it again to extend session if needed
        // or if rotation was added. for now just return access token in body.
        
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
