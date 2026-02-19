package com.orthowatch.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${app.jwt.expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey key;
    private SecretKey refreshKey;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }

    public String generateAccessToken(String username, String role, String fullName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("fullName", fullName);
        return createToken(claims, username, accessExpiration, key);
    }

    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshExpiration, refreshKey);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration, SecretKey signKey) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signKey)
                .compact();
    }

    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token, key));
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token, refreshKey);
            return (extractedUsername.equals(username) && !isTokenExpired(token, refreshKey));
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid Refresh token: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, key);
    }

    public String extractUsername(String token, SecretKey signKey) {
        return extractClaim(token, Claims::getSubject, signKey);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration, key);
    }
    
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class), key);
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractClaim(token, Claims::getSubject, refreshKey);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, SecretKey signKey) {
        final Claims claims = extractAllClaims(token, signKey);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, SecretKey signKey) {
        return Jwts.parser()
                .verifyWith(signKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token, SecretKey signKey) {
        return extractExpiration(token, signKey).before(new Date());
    }

    private Date extractExpiration(String token, SecretKey signKey) {
        return extractClaim(token, Claims::getExpiration, signKey);
    }
}
