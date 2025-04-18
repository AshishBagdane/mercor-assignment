package com.mercor.assignment.sdc.service.mapper;

import com.mercor.assignment.sdc.domain.dto.JobDTO;
import com.mercor.assignment.sdc.domain.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for converting between Job entity and JobDTO Using MapStruct for automatic mapping implementation
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMapper extends SCDMapper<JobDTO, Job> {

  /**
   * Convert a Job entity to a JobDTO
   *
   * @param entity The Job entity to convert
   * @return The corresponding JobDTO
   */
  @Override
  JobDTO toDto(Job entity);

  /**
   * Convert a JobDTO to a Job entity
   *
   * @param dto The JobDTO to convert
   * @return The corresponding Job entity
   */
  @Override
  Job toEntity(JobDTO dto);

  /**
   * Update a Job entity with data from another Job entity
   *
   * @param sourceEntity The source entity containing updated data
   * @param targetEntity The target entity to update
   */
  @Override
  void updateEntityFromDto(@MappingTarget Job targetEntity, Job sourceEntity);
}