package com.mercor.assignment.scd.domain.core.repository;

import com.mercor.assignment.scd.domain.core.model.SCDEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base interface for all SCD repositories
 * Defines common operations for SCD entities
 *
 * @param <T> the entity type extending SCDEntity
 */
public interface SCDRepositoryBase<T extends SCDEntity> {

    /**
     * Find the latest version of an entity by ID
     *
     * @param id the entity ID
     * @return Optional containing the entity if found, empty otherwise
     */
    Optional<T> findLatestVersionById(String id);

    /**
     * Find all versions of an entity by ID
     *
     * @param id the entity ID
     * @return List of all versions, ordered by version descending
     */
    List<T> findAllVersionsById(String id);

    /**
     * Find a specific version of an entity by UID
     *
     * @param uid the entity version UID
     * @return Optional containing the entity if found, empty otherwise
     */
    Optional<T> findByUid(String uid);

    /**
     * Create a new version of an entity with updated fields
     *
     * @param latestVersion the latest version of the entity
     * @param fieldsToUpdate map of field names to new values
     * @return the newly created version
     */
    T createNewVersion(T latestVersion, Map<String, Object> fieldsToUpdate);

    /**
     * Create a fresh entity with new fields
     *
     * @param entity map of field names to new values
     * @return the newly created version
     */
    T createEntity(T entity);

    /**
     * Find latest versions of entities matching specified criteria
     *
     * @param criteria map of field names to values for filtering
     * @return List of entities matching the criteria
     */
    List<T> findLatestVersionsByCriteria(Map<String, Object> criteria);
}
