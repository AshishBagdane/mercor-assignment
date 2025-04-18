package com.mercor.assignment.sdc.repository;

import com.mercor.assignment.sdc.domain.entity.Job;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Job entity
 */
@Repository
public interface JobRepository extends SCDRepository<Job> {

  /**
   * Find jobs with a rate above the specified value (latest versions only)
   *
   * @param rate Minimum rate value
   * @return List of jobs with a rate above the specified value
   */
  @Query(value = "SELECT j FROM Job j WHERE j.rate > :rate " +
      "AND j.version IN " +
      "(SELECT MAX(v.version) FROM Job v WHERE v.id = j.id GROUP BY v.id)")
  List<Job> findJobsWithRateAbove(@Param("rate") Double rate);

  /**
   * Find active jobs for a specific company (latest versions only)
   *
   * @param companyId Company ID
   * @return List of active jobs for the company
   */
  @Query(value = "SELECT j FROM Job j WHERE j.companyId = :companyId AND j.status = 'active' " +
      "AND j.version IN " +
      "(SELECT MAX(v.version) FROM Job v WHERE v.id = j.id GROUP BY v.id)")
  List<Job> findActiveJobsForCompany(@Param("companyId") String companyId);

  /**
   * Find active jobs for a specific contractor (latest versions only)
   *
   * @param contractorId Contractor ID
   * @return List of active jobs for the contractor
   */
  @Query(value = "SELECT j FROM Job j WHERE j.contractorId = :contractorId AND j.status = 'active' " +
      "AND j.version IN " +
      "(SELECT MAX(v.version) FROM Job v WHERE v.id = j.id GROUP BY v.id)")
  List<Job> findActiveJobsForContractor(@Param("contractorId") String contractorId);
}