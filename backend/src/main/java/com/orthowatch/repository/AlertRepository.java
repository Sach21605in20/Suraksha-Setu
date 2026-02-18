package com.orthowatch.repository;

import com.orthowatch.model.Alert;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
  List<Alert> findByAssignedToIdAndStatus(UUID assignedToId, String status);

  List<Alert> findByEpisodeId(UUID episodeId);
}
