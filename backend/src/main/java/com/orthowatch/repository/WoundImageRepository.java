package com.orthowatch.repository;

import com.orthowatch.model.WoundImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WoundImageRepository extends JpaRepository<WoundImage, UUID> {
  List<WoundImage> findByEpisodeId(UUID episodeId);
}
