package com.orthowatch.mapper;

import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PatientMapper {

  @Mapping(source = "patientName", target = "fullName")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "fhirPatientId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Patient toEntity(EnrollmentRequest request);

  @Mapping(source = "patientName", target = "fullName")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "fhirPatientId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(EnrollmentRequest request, @MappingTarget Patient patient);
}
