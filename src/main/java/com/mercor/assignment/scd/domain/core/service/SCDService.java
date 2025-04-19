package com.mercor.assignment.scd.domain.core.service;

import com.mercor.assignment.scd.domain.core.model.SCDEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic service interface for SCD operations
 * Provides a clean abstraction for working with slowly changing dimension entities
 * 
 * @param <T> the specific entity type extending SCDEntity
 */
public interface SCDService<T extends SCDEntity> {

    /**
     * Find the latest version of an entity by its ID
     *
     * @param id the entity ID (not UID)
     * @return Optional containing the entity with the latest version if found
     */
    Optional<T> findLatestVersionById(String id);

    /**
     * Find all versions of an entity by its ID
     *
     * @param id the entity ID
     * @return a list of entity versions sorted by version (descending)
     */
    List<T> findAllVersionsById(String id);

    /**
     * Find an entity by its specific version UID
     *
     * @param uid the specific version UID
     * @return Optional containing the entity if found
     */
    Optional<T> findByUid(String uid);

    /**
     * Create a new version of an entity with updated fields
     *
     * @param id the entity ID
     * @param fieldsToUpdate map of field names to new values
     * @return the newly created entity version
     * @throws com.mercor.assignment.scd.domain.core.exception.EntityNotFoundException if the specified entity does not exist
     */
    T createNewVersion(String id, Map<String, Object> fieldsToUpdate);

    /**
     * Create a new entity with an initial version
     *
     * @param entity the entity to create
     * @return the created entity
     */
    T createEntity(T entity);

    /**
     * Find the latest versions of entities matching the provided criteria
     *
     * @param criteria a map of field names to values for filtering
     * @return a list of entities matching the criteria (latest versions only)
     */
    List<T> findLatestVersionsByCriteria(Map<String, Object> criteria);
}