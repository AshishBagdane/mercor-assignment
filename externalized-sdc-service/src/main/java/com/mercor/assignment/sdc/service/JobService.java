package com.mercor.assignment.sdc.service;

import com.mercor.assignment.sdc.domain.dto.JobDTO;
import java.util.List;

/**
 * Service interface for Job entity operations
 */
public interface JobService extends SCDService<JobDTO> {

  /**
   * Get active jobs for a specific company
   *
   * @param companyId Company ID
   * @return List of active jobs for the company (latest versions only)
   */
  List<JobDTO> getActiveJobsForCompany(String companyId);

  /**
   * Get active jobs for a specific contractor
   *
   * @param contractorId Contractor ID
   * @return List of active jobs for the contractor (latest versions only)
   */
  List<JobDTO> getActiveJobsForContractor(String contractorId);

  /**
   * Get jobs with a rate above the specified value
   *
   * @param rate Minimum rate value
   * @return List of jobs with a rate above the specified value (latest versions only)
   */
  List<JobDTO> getJobsWithRateAbove(Double rate);

  /**
   * Update the status of a job
   *
   * @param id     Job ID
   * @param status New status
   * @return The newly created job version with the updated status
   */
  JobDTO updateStatus(String id, String status);

  /**
   * Update the rate of a job
   *
   * @param id   Job ID
   * @param rate New rate
   * @return The newly created job version with the updated rate
   */
  JobDTO updateRate(String id, Double rate);
}