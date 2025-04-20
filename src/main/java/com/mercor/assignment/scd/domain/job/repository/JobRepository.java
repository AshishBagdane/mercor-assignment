package com.mercor.assignment.scd.domain.job.repository;

import com.mercor.assignment.scd.domain.core.repository.SCDRepositoryBase;
import com.mercor.assignment.scd.domain.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Job entity
 * Extends both JpaRepository (for standard JPA operations) and
 * SCDRepositoryBase (for SCD-specific operations)
 */
@Repository
public interface JobRepository extends JpaRepository<Job, String>,
    JpaSpecificationExecutor<Job>,
    SCDRepositoryBase<Job> {
  // No additional methods needed - all are inherited from the parent interfaces
}