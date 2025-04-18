package com.mercor.assignment.sdc.service.impl;

import com.mercor.assignment.sdc.domain.dto.SCDBatchRequest;
import com.mercor.assignment.sdc.domain.dto.SCDBatchResponse;
import com.mercor.assignment.sdc.domain.dto.SCDEntityDTO;
import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import com.mercor.assignment.sdc.domain.entity.SCDEntity;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.repository.SCDRepository;
import com.mercor.assignment.sdc.service.SCDService;
import com.mercor.assignment.sdc.service.mapper.SCDMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract implementation of the SCDService interface Provides common
 * functionality for all SCD entity services
 *
 * @param <T> The DTO type for the entity
 * @param <E> The entity type
 */
public abstract class AbstractSCDService<T extends SCDEntityDTO, E extends SCDEntity> implements SCDService<T> {

  protected final SCDRepository<E> repository;
  protected final SCDMapper<T, E> mapper;

  protected AbstractSCDService(SCDRepository<E> repository, SCDMapper<T, E> mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public T getLatestVersion(String id) {
    E entity = repository.findFirstByIdOrderByVersionDesc(id)
        .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
    return mapper.toDto(entity);
  }

  @Override
  public List<T> getVersionHistory(String id) {
    List<E> entities = repository.findByIdOrderByVersionDesc(id);
    if (entities.isEmpty()) {
      throw new EntityNotFoundException("Entity not found with ID: " + id);
    }
    return entities.stream().map(mapper::toDto).collect(Collectors.toList());
  }

  @Override
  public T getSpecificVersion(String id, Integer version) {
    E entity = repository.findByIdAndVersion(id, version)
        .orElseThrow(() -> new EntityNotFoundException(
            String.format("Entity not found with ID: %s and version: %d", id, version)));
    return mapper.toDto(entity);
  }

  @Override
  public List<T> query(SCDQueryRequest queryRequest) {
    Specification<E> spec = createSpecification(queryRequest);

    Sort sort = Sort.unsorted();
    if (queryRequest.getSortBy() != null) {
      sort = Sort.by(
          "asc".equalsIgnoreCase(queryRequest.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC,
          queryRequest.getSortByFieldForJpa());
    }

    List<E> results;
    try {
      if (queryRequest.isLatestVersionOnly()) {
        results = repository.findLatestVersions(spec, sort,
            queryRequest.getOffset(), queryRequest.getLimit());
      } else {
        results = repository.findAll(spec, sort);
      }
    } catch (Exception e) {
      throw new SCDException("Error executing query: " + e.getMessage(), e, "QUERY_ERROR");
    }

    return results.stream().map(mapper::toDto).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public T create(T dto) {
    E entity = mapper.toEntity(dto);
    entity.setVersion(1);
    entity.setUid(generateUid(entity));
    entity.setCreatedAt(new Date());
    entity.setUpdatedAt(new Date());

    E savedEntity = repository.save(entity);
    return mapper.toDto(savedEntity);
  }

  @Override
  @Transactional
  public T update(String id, SCDUpdateRequest<T> updateRequest) {
    try {
      // Validate request
      if (id == null || id.isEmpty()) {
        throw new SCDException("Entity ID cannot be null or empty", "INVALID_ENTITY_ID");
      }

      if ((updateRequest.getEntity() == null || updateRequest.getEntity().getId() == null) &&
          (updateRequest.getFields() == null || updateRequest.getFields().isEmpty())) {
        throw new SCDException("Update request must contain either entity or fields", "INVALID_UPDATE_REQUEST");
      }

      // Check if the entity exists
      boolean entityExists = false;
      try {
        entityExists = repository.findFirstByIdOrderByVersionDesc(id).isPresent();
      } catch (Exception e) {
        // Ignore exceptions and treat as entity not existing
      }

      if (!entityExists) {
        // Entity doesn't exist, create a new one (upsert behavior)
        if (updateRequest.getEntity() != null) {
          T dto = updateRequest.getEntity();
          // Ensure the ID is set correctly
          dto.setId(id);
          return create(dto);
        } else {
          // Cannot create a new entity with just fields
          throw new EntityNotFoundException(
              "Entity not found with ID: " + id + " and cannot be created with just fields");
        }
      }

      // Entity exists, fetch the latest version
      E latestEntity = repository.findFirstByIdOrderByVersionDesc(id)
          .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));

      // Create new version
      E newVersionEntity = createNewVersion(latestEntity);

      // Apply updates
      if (updateRequest.getEntity() != null) {
        E updatedEntity = mapper.toEntity(updateRequest.getEntity());
        // Ensure we don't override ID or version from the request
        updatedEntity.setId(id);
        updatedEntity.setVersion(newVersionEntity.getVersion());
        mapper.updateEntityFromDto(updatedEntity, newVersionEntity);
      } else if (updateRequest.getFields() != null && !updateRequest.getFields().isEmpty()) {
        updateFields(newVersionEntity, updateRequest.getFields());
      }

      // Set metadata
      newVersionEntity.setUid(generateUid(newVersionEntity));
      newVersionEntity.setCreatedAt(new Date());
      newVersionEntity.setUpdatedAt(new Date());

      // Save entity
      E savedEntity = repository.save(newVersionEntity);
      return mapper.toDto(savedEntity);
    } catch (org.springframework.dao.OptimisticLockingFailureException e) {
      throw new SCDException("Entity was modified by another transaction. Please retry with latest version.", e,
          "CONCURRENT_MODIFICATION");
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new SCDException("Data integrity violation during update. Check unique constraints.", e,
          "DATA_INTEGRITY_VIOLATION");
    } catch (EntityNotFoundException e) {
      throw e;
    } catch (SCDException e) {
      throw e;
    } catch (Exception e) {
      throw new SCDException("Unexpected error during entity update: " + e.getMessage(), e, "UPDATE_ERROR");
    }
  }

  @Override
  @Transactional
  public SCDBatchResponse<T> batchGet(SCDBatchRequest batchRequest) {
    if (batchRequest.getIds() == null || batchRequest.getIds().isEmpty()) {
      throw new SCDException("Batch get request must contain IDs", "INVALID_BATCH_REQUEST");
    }

    List<E> entities = repository.findLatestVersionsByIds(batchRequest.getIds());
    List<T> dtos = entities.stream().map(mapper::toDto).collect(Collectors.toList());

    Map<String, String> errors = new HashMap<>();
    Set<String> foundIds = entities.stream().map(SCDEntity::getId).collect(Collectors.toSet());
    batchRequest.getIds().forEach(id -> {
      if (!foundIds.contains(id)) {
        errors.put(id.toString(), "Entity not found");
      }
    });

    return SCDBatchResponse.<T>builder()
        .entities(dtos)
        .errors(errors)
        .successCount(dtos.size())
        .failureCount(errors.size())
        .build();
  }

  @Override
  @Transactional
  public SCDBatchResponse<T> batchUpdate(SCDBatchRequest<T> batchUpdateRequest) {
    if (batchUpdateRequest.getEntities() == null || batchUpdateRequest.getEntities().isEmpty()) {
      throw new SCDException("Batch update request must contain entities", "INVALID_BATCH_REQUEST");
    }

    List<T> updatedEntities = new ArrayList<>();
    Map<String, String> errors = new HashMap<>();

    // Validate all entities before processing
    for (T dto : batchUpdateRequest.getEntities()) {
      if (dto.getId() == null || dto.getId().isEmpty()) {
        errors.put(dto.getUid() != null ? dto.getUid() : "unknown",
            "Entity ID cannot be null or empty");
      }
    }

    // If we already have validation errors, return early
    if (!errors.isEmpty()) {
      return SCDBatchResponse.<T>builder()
          .entities(updatedEntities)
          .errors(errors)
          .successCount(0)
          .failureCount(errors.size())
          .build();
    }

    // Process entities one by one for better error handling
    for (T dto : batchUpdateRequest.getEntities()) {
      try {
        SCDUpdateRequest<T> updateRequest = new SCDUpdateRequest<>();
        updateRequest.setEntity(dto);

        if (batchUpdateRequest.getCommonFields() != null && !batchUpdateRequest.getCommonFields().isEmpty()) {
          updateRequest.setFields(new HashMap<>(batchUpdateRequest.getCommonFields()));
        }

        T updatedEntity = update(dto.getId(), updateRequest);
        updatedEntities.add(updatedEntity);
      } catch (Exception e) {
        String errorMessage = e.getMessage();
        if (e instanceof SCDException) {
          errorMessage = ((SCDException) e).getErrorCode() + ": " + e.getMessage();
        }
        errors.put(dto.getId(), errorMessage);
      }
    }

    return SCDBatchResponse.<T>builder()
        .entities(updatedEntities)
        .errors(errors)
        .successCount(updatedEntities.size())
        .failureCount(errors.size())
        .build();
  }

  /**
   * Create a specification based on the query request
   *
   * @param queryRequest The query request
   * @return The specification
   */
  protected abstract Specification<E> createSpecification(SCDQueryRequest queryRequest);

  /**
   * Create a new version of an entity based on the latest version
   *
   * @param latestEntity The latest version of the entity
   * @return A new entity version
   */
  protected E createNewVersion(E latestEntity) {
    E newVersionEntity;
    try {
      newVersionEntity = (E) latestEntity.getClass().getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new SCDException("Failed to create new entity version", e);
    }

    // Copy all properties except version, uid, and dates
    newVersionEntity.setId(latestEntity.getId());
    newVersionEntity.setVersion(latestEntity.getVersion() + 1);

    // Use reflection to copy other properties
    copyProperties(latestEntity, newVersionEntity);

    return newVersionEntity;
  }

  /**
   * Update entity fields using a map of field names and values
   *
   * @param entity The entity to update
   * @param fields Map of field names and values
   */
  protected abstract void updateFields(E entity, Map<String, Object> fields);

  /**
   * Generate a new UID for an entity
   *
   * @param entity The entity
   * @return A new UID
   */
  protected String generateUid(E entity) {
    String entityType = entity.getClass().getSimpleName().toLowerCase();
    return entityType + "_uid_" + UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Copy properties from one entity to another
   *
   * @param source The source entity
   * @param target The target entity
   */
  protected abstract void copyProperties(E source, E target);
}