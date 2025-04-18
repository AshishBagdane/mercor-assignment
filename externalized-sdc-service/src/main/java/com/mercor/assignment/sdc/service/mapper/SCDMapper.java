package com.mercor.assignment.sdc.service.mapper;

import com.mercor.assignment.sdc.domain.dto.SCDEntityDTO;
import com.mercor.assignment.sdc.domain.entity.SCDEntity;

/**
 * Base mapper interface for converting between entity and DTO objects
 *
 * @param <D> The DTO type
 * @param <E> The entity type
 */
public interface SCDMapper<D extends SCDEntityDTO, E extends SCDEntity> {

  /**
   * Convert an entity to a DTO
   *
   * @param entity The entity to convert
   * @return The corresponding DTO
   */
  D toDto(E entity);

  /**
   * Convert a DTO to an entity
   *
   * @param dto The DTO to convert
   * @return The corresponding entity
   */
  E toEntity(D dto);

  /**
   * Update an entity with data from a DTO This is used during updates to transfer DTO fields to the entity
   *
   * @param sourceDto    The source DTO containing updated data
   * @param targetEntity The target entity to update
   */
  void updateEntityFromDto(E sourceEntity, E targetEntity);
}