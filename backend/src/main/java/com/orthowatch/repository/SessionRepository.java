package com.orthowatch.repository;

import com.orthowatch.model.Session;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
  Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

  void deleteByUserId(UUID userId);
}
