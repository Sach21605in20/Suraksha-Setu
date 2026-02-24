package com.orthowatch.repository;

import com.orthowatch.model.Episode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
  List<Episode> findByPatientId(UUID patientId);

  List<Episode> findByPrimarySurgeonId(UUID primarySurgeonId);

  List<Episode> findByStatus(String status);

  boolean existsByPatientIdAndTemplateSurgeryTypeAndStatus(
      UUID patientId, String surgeryType, String status);
}
