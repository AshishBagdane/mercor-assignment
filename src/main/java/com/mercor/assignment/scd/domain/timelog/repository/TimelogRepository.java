package com.mercor.assignment.scd.domain.timelog.repository;

import com.mercor.assignment.scd.domain.core.repository.SCDRepositoryBase;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Job entity
 * Extends both JpaRepository (for standard JPA operations) and
 * SCDRepositoryBase (for SCD-specific operations)
 */
@Repository
public interface TimelogRepository extends JpaRepository<Timelog, String>,
    JpaSpecificationExecutor<Timelog>,
    SCDRepositoryBase<Timelog> {

  List<Timelog> findByJobUid(String jobUid);

  List<Timelog> findByDurationGreaterThan(Long minDuration);

  List<Timelog> findByJobUidInAndTimeStartGreaterThanEqualAndTimeEndLessThanEqual(List<String> jobUids, Long startTime, Long endTime);
  // No additional methods needed - all are inherited from the parent interfaces
}