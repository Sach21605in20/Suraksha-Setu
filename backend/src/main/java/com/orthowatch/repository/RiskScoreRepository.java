package com.orthowatch.repository;

import com.orthowatch.model.RiskScore;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {
  List<RiskScore> findByEpisodeId(UUID episodeId);

  List<RiskScore> findByEpisodeIdOrderByDayNumberDesc(UUID episodeId);
}
