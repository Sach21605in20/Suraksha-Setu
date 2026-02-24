package com.orthowatch.mapper;

import com.orthowatch.dto.EnrollmentRequest;
import com.orthowatch.model.Episode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EpisodeMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "patient", ignore = true)
  @Mapping(target = "template", ignore = true)
  @Mapping(target = "primarySurgeon", ignore = true)
  @Mapping(target = "secondaryClinician", ignore = true)
  @Mapping(target = "currentDay", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "consentStatus", ignore = true)
  @Mapping(target = "consentTimestamp", ignore = true)
  @Mapping(target = "checklistTime", ignore = true)
  @Mapping(target = "timezone", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Episode toEntity(EnrollmentRequest request);
}
