package com.mercor.assignment.sdc.service.mapper;

import com.mercor.assignment.sdc.domain.dto.TimelogDTO;
import com.mercor.assignment.sdc.domain.entity.Timelog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for converting between Timelog entity and TimelogDTO Using MapStruct for automatic mapping implementation
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelogMapper extends SCDMapper<TimelogDTO, Timelog> {

  /**
   * Convert a Timelog entity to a TimelogDTO
   *
   * @param entity The Timelog entity to convert
   * @return The corresponding TimelogDTO
   */
  @Override
  TimelogDTO toDto(Timelog entity);

  /**
   * Convert a TimelogDTO to a Timelog entity
   *
   * @param dto The TimelogDTO to convert
   * @return The corresponding Timelog entity
   */
  @Override
  Timelog toEntity(TimelogDTO dto);

  /**
   * Update a Timelog entity with data from another Timelog entity
   *
   * @param sourceEntity The source entity containing updated data
   * @param targetEntity The target entity to update
   */
  @Override
  void updateEntityFromDto(@MappingTarget Timelog targetEntity, Timelog sourceEntity);
}