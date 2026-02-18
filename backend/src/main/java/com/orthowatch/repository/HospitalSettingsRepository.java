package com.orthowatch.repository;

import com.orthowatch.model.HospitalSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalSettingsRepository extends JpaRepository<HospitalSettings, UUID> {}
