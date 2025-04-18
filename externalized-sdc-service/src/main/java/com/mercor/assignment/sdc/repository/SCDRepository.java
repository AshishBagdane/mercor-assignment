package com.mercor.assignment.sdc.repository;

import com.mercor.assignment.sdc.domain.entity.SCDEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * Base repository interface for SCD entities
 *
 * @param <T> The entity type
 */
@NoRepositoryBean
public interface SCDRepository<T extends SCDEntity> extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    /**
     * Find an entity by ID and version
     *
     * @param id      Entity ID
     * @param version Version number
     * @return Optional containing the entity if found
     */
    Optional<T> findByIdAndVersion(String id, Integer version);

    /**
     * Find the latest version of an entity by ID
     *
     * @param id Entity ID
     * @return Optional containing the entity if found
     */
    Optional<T> findFirstByIdOrderByVersionDesc(String id);

    /**
     * Find all versions of an entity by ID, ordered by version (descending)
     *
     * @param id Entity ID
     * @return List of all versions of the entity
     */
    List<T> findByIdOrderByVersionDesc(String id);

    /**
     * Find the latest versions of entities matching a specification
     *
     * @param spec   Specification to filter entities
     * @param sort   Sort criteria
     * @param offset Offset for pagination
     * @param limit  Limit for pagination
     * @return List of latest versions of entities matching the specification
     */
    @Query(value = "SELECT e FROM #{#entityName} e WHERE e.id IN " +
            "(SELECT DISTINCT t.id FROM #{#entityName} t WHERE (:spec IS NULL OR :spec = true)) " +
            "AND e.version IN " +
            "(SELECT MAX(v.version) FROM #{#entityName} v WHERE v.id = e.id GROUP BY v.id)")
    List<T> findLatestVersions(@Param("spec") Specification<T> spec, Sort sort,
            @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * Find the latest versions of entities with the specified IDs
     *
     * @param ids List of entity IDs
     * @return List of latest versions of entities with the specified IDs
     */
    @Query(value = "SELECT e FROM #{#entityName} e WHERE e.id IN :ids " +
            "AND e.version IN " +
            "(SELECT MAX(v.version) FROM #{#entityName} v WHERE v.id = e.id GROUP BY v.id)")
    List<T> findLatestVersionsByIds(@Param("ids") List<String> ids);
}