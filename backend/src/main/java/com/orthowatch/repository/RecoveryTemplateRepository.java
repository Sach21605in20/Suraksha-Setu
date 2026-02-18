package com.orthowatch.repository;

import com.orthowatch.model.RecoveryTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryTemplateRepository extends JpaRepository<RecoveryTemplate, UUID> {
  Optional<RecoveryTemplate> findBySurgeryType(String surgeryType);
}
