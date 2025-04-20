package com.mercor.assignment.scd.domain.core.repository.impl;

import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.core.repository.SCDRepositoryBase;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation of the SCDRepositoryBase interface
 * Provides common functionality for all SCD repositories
 *
 * @param <T> the entity type extending SCDEntity
 */
public abstract class AbstractSCDRepositoryImpl<T extends SCDEntity> implements SCDRepositoryBase<T> {

    @PersistenceContext
    protected EntityManager entityManager;

    protected final UidGenerator uidGenerator;
    protected final Class<T> entityClass;
    protected final String entityTypeName;

    /**
     * Constructor with required dependencies
     *
     * @param uidGenerator the UID generator utility
     * @param entityClass the entity class
     * @param entityType the entity type enum
     */
    protected AbstractSCDRepositoryImpl(UidGenerator uidGenerator, Class<T> entityClass, EntityType entityType) {
        this.uidGenerator = uidGenerator;
        this.entityClass = entityClass;
        this.entityTypeName = entityType.getPrefix();
    }

    @Override
    public Optional<T> findLatestVersionById(String id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        // Add condition for ID
        Predicate idPredicate = cb.equal(root.get("id"), id);

        // Create a subquery to get the max version for this ID
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<T> subRoot = subquery.from(entityClass);
        subquery.select(cb.max(subRoot.get("version")))
            .where(cb.equal(subRoot.get("id"), id));

        // Add condition to match the max version
        Predicate versionPredicate = cb.equal(root.get("version"), subquery);

        // Combine conditions
        query.where(cb.and(idPredicate, versionPredicate));

        // Execute query
        List<T> results = entityManager.createQuery(query).getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<T> findAllVersionsById(String id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        // Add condition for ID
        query.where(cb.equal(root.get("id"), id));

        // Add ordering by version (descending)
        query.orderBy(cb.desc(root.get("version")));

        // Execute query
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Optional<T> findByUid(String uid) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        query.where(cb.equal(root.get("uid"), uid));
        List<T> results = entityManager.createQuery(query).getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @Transactional
    public T createNewVersion(T latestVersion, Map<String, Object> fieldsToUpdate) {
        // Generate the new UID for this version
        String newUid = uidGenerator.generateUid(entityTypeName);
        Date now = new Date();

        // Create a new version using the clone method
        @SuppressWarnings("unchecked")
        T newVersion = (T) latestVersion.cloneForNewVersion(newUid, latestVersion.getVersion() + 1, now);

        // Update fields based on the provided map
        updateEntityFields(newVersion, fieldsToUpdate);

        // Merge the new version (instead of persist)
        newVersion = entityManager.merge(newVersion);
        entityManager.flush(); // Force immediate persistence

        return newVersion;
    }

    @Override
    public List<T> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        // Create predicates for all criteria
        List<Predicate> predicates = new ArrayList<>();
        criteria.forEach((field, value) -> {
            predicates.add(cb.equal(root.get(field), value));
        });

        // Create a subquery to get the max version for each ID
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<T> subRoot = subquery.from(entityClass);
        subquery.select(cb.max(subRoot.get("version")))
            .where(cb.equal(subRoot.get("id"), root.get("id")));

        // Add condition to match the max version
        predicates.add(cb.equal(root.get("version"), subquery));

        // Combine all predicates
        query.where(cb.and(predicates.toArray(new Predicate[0])));

        // Add order by updated_at desc
        query.orderBy(cb.desc(root.get("updatedAt")));

        // Execute query
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Create an empty entity instance
     *
     * @return a new empty entity instance
     */
    protected abstract T createEmptyEntity();

    /**
     * Update entity fields based on the map
     *
     * @param entity the entity to update
     * @param fieldsToUpdate map of field names to new values
     */
    protected abstract void updateEntityFields(T entity, Map<String, Object> fieldsToUpdate);
}