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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Abstract implementation of the SCDService interface Provides common
 * functionality for all SCD entity services
 *
 * @param <T> The DTO type for the entity
 * @param <E> The entity type
 */
public abstract class AbstractSCDService<T extends SCDEntityDTO, E extends SCDEntity> implements SCDService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractSCDService.class);
  private static final int MAX_RETRIES = 3;

  private final SCDRepository<E> repository;
  private final SCDMapper<T, E> mapper;

  public AbstractSCDService(SCDRepository<E> repository, SCDMapper<T, E> mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public T getLatestVersion(String id) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Entity ID cannot be null or empty", "INVALID_ENTITY_ID");
    }
    E entity = repository.findFirstByIdOrderByVersionDesc(id)
        .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
    return mapper.toDto(entity);
  }

  @Override
  public List<T> getVersionHistory(String id) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Entity ID cannot be null or empty", "INVALID_ENTITY_ID");
    }
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
        // Get all latest versions first
        List<E> latestVersions = repository.findLatestVersions(null, sort,
            queryRequest.getOffset(), queryRequest.getLimit());

        // Then manually apply the specification if one was provided
        if (spec != null) {
          results = repository.findAll(spec);

          // Only keep entities that are both in the latest versions and match the
          // specification
          Set<String> latestIds = latestVersions.stream()
              .map(e -> e.getId() + "_" + e.getVersion())
              .collect(Collectors.toSet());

          results = results.stream()
              .filter(e -> latestIds.contains(e.getId() + "_" + e.getVersion()))
              .collect(Collectors.toList());
        } else {
          results = latestVersions;
        }
      } else {
        results = repository.findAll(spec, sort);
      }
    } catch (Exception e) {
      log.error("Error executing query: {}", e.getMessage(), e);
      throw new SCDException("Error executing query: " + e.getMessage(), e, "QUERY_ERROR");
    }

    return results.stream().map(mapper::toDto).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public T create(T dto) {
    if (dto == null) {
      throw new SCDException("Entity cannot be null", "INVALID_ENTITY");
    }

    // Check if entity ID is provided
    if (dto.getId() == null || dto.getId().isEmpty()) {
      // Generate a new ID if not provided
      dto.setId(generateEntityId());
    }

    // Set initial values
    E entity = mapper.toEntity(dto);
    entity.setVersion(1);
    entity.setUid(generateUid(entity));
    entity.setCreatedAt(new Date());
    entity.setUpdatedAt(new Date());

    try {
      E savedEntity = repository.save(entity);
      return mapper.toDto(savedEntity);
    } catch (Exception e) {
      log.error("Error creating entity: {}", e.getMessage(), e);
      throw new SCDException("Error creating entity: " + e.getMessage(), e, "CREATE_ERROR");
    }
  }

  @Override
  @Transactional
  public T update(String id, SCDUpdateRequest<T> updateRequest) {
    int retries = 0;
    int maxRetries = MAX_RETRIES * 2; // Double the retries for concurrent issues

    while (true) {
      try {
        return executeUpdate(id, updateRequest);
      } catch (OptimisticLockingFailureException e) {
        if (retries < maxRetries) {
          retries++;
          log.warn("Entity was modified by another transaction. Retrying ({}/{})", retries, maxRetries);
          try {
            // Better exponential backoff with more jitter
            long sleepTime = (long) (Math.pow(2, retries) * 50 + Math.random() * 100 * retries);
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new SCDException("Update interrupted", ie, "UPDATE_INTERRUPTED");
          }
        } else {
          throw new SCDException("Entity was modified by another transaction. Please retry with latest version.",
              e, "CONCURRENT_MODIFICATION");
        }
      }
    }
  }

  @Transactional
  private T executeUpdate(String id, SCDUpdateRequest<T> updateRequest) {
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
      Optional<E> existingEntityOpt = repository.findFirstByIdOrderByVersionDesc(id);
      boolean entityExists = existingEntityOpt.isPresent();

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
      E latestEntity = existingEntityOpt.get();

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
      throw e; // Re-throw to be caught by the retry mechanism
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

    // Process entities individually with a much more isolated approach
    for (T dto : batchUpdateRequest.getEntities()) {
      try {
        // Create a completely isolated update request for each entity
        SCDUpdateRequest<T> singleEntityRequest = new SCDUpdateRequest<>();
        singleEntityRequest.setEntity(dto);

        if (batchUpdateRequest.getCommonFields() != null && !batchUpdateRequest.getCommonFields().isEmpty()) {
          singleEntityRequest.setFields(new HashMap<>(batchUpdateRequest.getCommonFields()));
        }

        // Use a completely new transaction with REQUIRES_NEW propagation
        T updatedEntity = processEntityWithNewTransaction(dto.getId(), singleEntityRequest);
        updatedEntities.add(updatedEntity);
      } catch (Exception e) {
        String errorMessage = e.getMessage();
        if (e instanceof SCDException) {
          errorMessage = ((SCDException) e).getErrorCode() + ": " + e.getMessage();
        }
        errors.put(dto.getId(), errorMessage);
        log.error("Error updating entity {} in batch: {}", dto.getId(), errorMessage, e);
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
   * Process an entity update in a completely new transaction to ensure isolation
   * 
   * @param id            The entity ID
   * @param updateRequest The update request
   * @return The updated entity
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  protected T processEntityWithNewTransaction(String id, SCDUpdateRequest<T> updateRequest) {
    try {
      // Try to execute the update with retries for concurrent modifications
      int retries = 0;
      int maxRetries = MAX_RETRIES * 2;

      while (true) {
        try {
          return executeUpdate(id, updateRequest);
        } catch (OptimisticLockingFailureException e) {
          if (retries < maxRetries) {
            retries++;
            log.warn("Entity was modified by another transaction in batch. Retrying ({}/{})", retries, maxRetries);
            try {
              // Better exponential backoff with more jitter
              long sleepTime = (long) (Math.pow(2, retries) * 50 + Math.random() * 100 * retries);
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              throw new SCDException("Update interrupted", ie, "UPDATE_INTERRUPTED");
            }
          } else {
            throw new SCDException(
                "Entity was modified by another transaction in batch. Please retry with latest version.",
                e, "CONCURRENT_MODIFICATION");
          }
        }
      }
    } catch (Exception e) {
      // Ensure the exception doesn't cause the whole batch to roll back
      log.error("Error in isolated transaction for entity {}: {}", id, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Create a new version of an entity for update operations
   * 
   * @param entity The entity to create a new version from
   * @return The new version entity
   */
  protected E createNewVersion(E entity) {
    try {
      @SuppressWarnings("unchecked")
      E newEntity = (E) entity.getClass().getDeclaredConstructor().newInstance();
      newEntity.setId(entity.getId());
      newEntity.setVersion(entity.getVersion() + 1);
      copyProperties(entity, newEntity);
      return newEntity;
    } catch (Exception e) {
      throw new SCDException("Error creating new version: " + e.getMessage(), e, "VERSION_CREATION_ERROR");
    }
  }

  /**
   * Generate a unique ID for a new entity
   * 
   * @return The generated ID
   */
  protected String generateEntityId() {
    // Generate a random ID with a prefix based on entity type
    String prefix = getEntityTypePrefix();
    String randomId = UUID.randomUUID().toString().substring(0, 20).replace("-", "");
    return prefix + "_" + randomId;
  }

  /**
   * Get the entity type prefix for ID generation
   * 
   * @return The entity type prefix
   */
  protected String getEntityTypePrefix() {
    // Default implementation using class name
    String className = this.getClass().getSimpleName().toLowerCase();
    if (className.endsWith("serviceimpl")) {
      className = className.substring(0, className.length() - 11);
    }
    return className;
  }

  /**
   * Generate a unique UID for an entity version
   * 
   * @param entity The entity to generate a UID for
   * @return The generated UID
   */
  protected String generateUid(E entity) {
    return entity.getId() + "_v" + entity.getVersion() + "_" + UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * Create a specification for querying entities based on the query request
   * 
   * @param queryRequest The query request
   * @return The specification
   */
  protected abstract Specification<E> createSpecification(SCDQueryRequest queryRequest);

  /**
   * Update entity fields based on the provided field map
   * 
   * @param entity The entity to update
   * @param fields The fields to update
   */
  protected abstract void updateFields(E entity, Map<String, Object> fields);

  /**
   * Copy properties from one entity to another for versioning
   * 
   * @param source The source entity
   * @param target The target entity
   */
  protected abstract void copyProperties(E source, E target);
}