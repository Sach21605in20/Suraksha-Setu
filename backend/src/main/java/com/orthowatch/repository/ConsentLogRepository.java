package com.orthowatch.repository;

import com.orthowatch.model.ConsentLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {
  List<ConsentLog> findByEpisodeId(UUID episodeId);
}
