package com.orthowatch.repository;

import com.orthowatch.model.Patient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
  Optional<Patient> findByPhonePrimary(String phonePrimary);

  Optional<Patient> findByHospitalMrn(String hospitalMrn);
}
