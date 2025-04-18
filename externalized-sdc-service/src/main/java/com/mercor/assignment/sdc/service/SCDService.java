package com.mercor.assignment.sdc.service;

import com.mercor.assignment.sdc.domain.dto.SCDBatchRequest;
import com.mercor.assignment.sdc.domain.dto.SCDBatchResponse;
import com.mercor.assignment.sdc.domain.dto.SCDEntityDTO;
import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import java.util.List;

/**
 * Base service interface for SCD operations
 *
 * @param <T> The DTO type for the entity
 */
public interface SCDService<T extends SCDEntityDTO> {

  /**
   * Get the latest version of an entity by ID
   *
   * @param id Entity ID
   * @return The latest version of the entity
   */
  T getLatestVersion(String id);

  /**
   * Get the complete history of an entity by ID
   *
   * @param id Entity ID
   * @return List of all versions of the entity, ordered by version (descending)
   */
  List<T> getVersionHistory(String id);

  /**
   * Get a specific version of an entity
   *
   * @param id      Entity ID
   * @param version Version number
   * @return The specified version of the entity
   */
  T getSpecificVersion(String id, Integer version);

  /**
   * Query entities based on specified criteria
   *
   * @param queryRequest Query parameters and conditions
   * @return List of entities matching the criteria
   */
  List<T> query(SCDQueryRequest queryRequest);

  /**
   * Create a new entity (version 1)
   *
   * @param entity The entity to create
   * @return The created entity
   */
  T create(T entity);

  /**
   * Update an entity, creating a new version
   *
   * @param id            Entity ID
   * @param updateRequest Update data
   * @return The newly created entity version
   */
  T update(String id, SCDUpdateRequest<T> updateRequest);

  /**
   * Batch get latest versions of multiple entities
   *
   * @param batchRequest Request containing IDs of entities to retrieve
   * @return Response containing retrieved entities
   */
  SCDBatchResponse<T> batchGet(SCDBatchRequest batchRequest);

  /**
   * Batch update multiple entities, creating new versions for each
   *
   * @param batchUpdateRequest Request containing entities to update
   * @return Response containing newly created entity versions
   */
  SCDBatchResponse<T> batchUpdate(SCDBatchRequest<T> batchUpdateRequest);
}