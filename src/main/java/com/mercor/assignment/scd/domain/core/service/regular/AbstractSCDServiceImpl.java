package com.mercor.assignment.scd.domain.core.service.regular;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.core.repository.SCDRepositoryBase;
import com.mercor.assignment.scd.domain.core.service.SCDService;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation of SCDService
 * Provides common functionality for all SCD services
 *
 * @param <T> the entity type extending SCDEntity
 * @param <R> the repository type extending SCDRepositoryBase
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractSCDServiceImpl<T extends SCDEntity, R extends SCDRepositoryBase<T>>
    implements SCDService<T> {

    protected final R repository;
    protected final UidGenerator uidGenerator;
    protected final EntityType entityType;

    @Override
    public Optional<T> findLatestVersionById(String id) {
        validateId(id);
        return repository.findLatestVersionById(id);
    }

    @Override
    public List<T> findAllVersionsById(String id) {
        validateId(id);
        return repository.findAllVersionsById(id);
    }

    @Override
    public Optional<T> findByUid(String uid) {
        validateUid(uid);
        return repository.findByUid(uid);
    }

    @Override
    @Transactional
    public T createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
        validateId(id);

        Optional<T> latestVersionOpt = repository.findLatestVersionById(id);
        if (latestVersionOpt.isEmpty()) {
            throw new EntityNotFoundException("Entity with ID " + id + " not found");
        }

        return repository.createNewVersion(latestVersionOpt.get(), fieldsToUpdate);
    }

    @Override
    @Transactional
    public T createEntity(T entity) {
        validateEntity(entity);
        // Generate a new entity ID if not set
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(uidGenerator.generateEntityId(entityType.getPrefix()));
        }

        // Set initial version
        entity.setVersion(1);

        // Generate UID for this version
        entity.setUid(uidGenerator.generateUid(entityType.getPrefix()));

        // Set timestamps
        final Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Validate entity before saving
        validateEntity(entity);

        // Save the entity
        return repository.createEntity(entity);
    }

    @Override
    public List<T> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return repository.findLatestVersionsByCriteria(criteria);
    }

    /**
     * Validate entity ID format
     */
    protected void validateId(String id) {
        if (!SCDValidators.SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid entity ID format");
        }
    }

    /**
     * Validate entity UID format
     */
    protected void validateUid(String uid) {
        if (!SCDValidators.SCDCommonValidators.validUid.isValid(uid)) {
            throw new ValidationException("Invalid entity UID format");
        }
    }

    /**
     * Validate the entity before saving
     * Entity-specific validation should be implemented by subclasses
     */
    protected abstract void validateEntity(T entity);
}
