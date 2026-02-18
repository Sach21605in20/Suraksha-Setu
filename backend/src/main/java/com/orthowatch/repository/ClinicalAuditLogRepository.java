package com.orthowatch.repository;

import com.orthowatch.model.ClinicalAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalAuditLogRepository extends JpaRepository<ClinicalAuditLog, UUID> {
  List<ClinicalAuditLog> findByUserId(UUID userId);

  List<ClinicalAuditLog> findByEpisodeId(UUID episodeId);
}
