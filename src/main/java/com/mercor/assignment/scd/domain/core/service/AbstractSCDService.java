package com.mercor.assignment.scd.domain.core.service;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation of SCDService that provides common functionality
 * for all entity types using the Template Method Pattern
 *
 * @param <T> the specific entity type extending SCDEntity
 * @param <R> the repository type extending JpaRepository
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractSCDService<T extends SCDEntity, R extends JpaRepository<T, String>> implements SCDService<T> {

    protected final R repository;
    protected final UidGenerator uidGenerator;
    protected final String entityType;

    @Override
    public Optional<T> findLatestVersionById(String id) {
        return findAllVersionsById(id).stream()
                .findFirst();
    }

    @Override
    public List<T> findAllVersionsById(String id) {
        List<T> versions = findAllVersionsByIdFromRepository(id);
        if (versions.isEmpty()) {
            return versions;
        }
        // Sort by version in descending order (latest first)
        return versions.stream()
                .sorted((e1, e2) -> Integer.compare(e2.getVersion(), e1.getVersion()))
                .toList();
    }

    @Override
    public Optional<T> findByUid(String uid) {
        return repository.findById(uid);
    }

    @Override
    @Transactional
    public T createEntity(T entity) {
        // Generate a new entity ID
        String id = uidGenerator.generateEntityId(entityType);
        entity.setId(id);
        entity.setVersion(1);
        
        // Generate a new UID for this version
        String uid = uidGenerator.generateUid(entityType);
        entity.setUid(uid);
        
        // Set timestamps
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
        T latestVersion = findLatestVersionById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity with ID " + id + " not found"));
        
        // Create a new version based on the latest version
        T newVersion = createNewVersionFromLatest(latestVersion);
        
        // Update the specified fields
        updateEntityFields(newVersion, fieldsToUpdate);
        
        // Increment version number
        newVersion.setVersion(latestVersion.getVersion() + 1);
        
        // Generate new UID for this version
        String uid = uidGenerator.generateUid(entityType);
        newVersion.setUid(uid);
        
        // Update timestamps
        newVersion.setUpdatedAt(new Date());
        
        return repository.save(newVersion);
    }

    @Override
    public List<T> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return executeLatestVersionsQuery(criteria);
    }

    /**
     * Abstract method to create a new entity version from an existing one
     * Implementations must define how entity-specific fields are copied
     *
     * @param latestVersion the latest version of the entity
     * @return a new entity instance with copied fields from the latest version
     */
    protected abstract T createNewVersionFromLatest(T latestVersion);

    /**
     * Abstract method to update entity fields
     * Implementations must define how fields are updated based on the map
     *
     * @param entity the entity to update
     * @param fieldsToUpdate map of field names to new values
     */
    protected abstract void updateEntityFields(T entity, Map<String, Object> fieldsToUpdate);

    /**
     * Abstract method to find all versions of an entity by ID
     * Implementations can optimize this query based on specific repository capabilities
     *
     * @param id the entity ID
     * @return list of all versions for the given entity ID
     */
    protected List<T> findAllVersionsByIdFromRepository(String id) {
        // Default implementation using Example matcher
        T probe = createEmptyEntity();
        probe.setId(id);
        
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withMatcher("id", ExampleMatcher.GenericPropertyMatchers.exact());
        
        return repository.findAll(Example.of(probe, matcher));
    }

    /**
     * Abstract method to execute a query returning only latest versions of entities
     * matching specific criteria
     *
     * @param criteria map of field names to values for filtering
     * @return list of latest versions matching criteria
     */
    protected abstract List<T> executeLatestVersionsQuery(Map<String, Object> criteria);

    /**
     * Create an empty entity instance
     * Used for query construction
     *
     * @return an empty entity instance
     */
    protected abstract T createEmptyEntity();
}