package com.mercor.assignment.scd.domain.job.service;

import com.mercor.assignment.scd.domain.core.service.SCDService;
import com.mercor.assignment.scd.domain.job.model.Job;

import java.util.List;

/**
 * Service interface for Job-specific operations
 * Extends the generic SCDService with Job-specific methods
 */
public interface JobService extends SCDService<Job> {

    /**
     * Find active jobs for a specific company
     *
     * @param companyId the company ID
     * @return list of active jobs for the company (latest versions only)
     */
    List<Job> findActiveJobsForCompany(String companyId);

    /**
     * Find active jobs for a specific contractor
     *
     * @param contractorId the contractor ID
     * @return list of active jobs for the contractor (latest versions only)
     */
    List<Job> findActiveJobsForContractor(String contractorId);

    /**
     * Find jobs with rate above the specified minimum
     *
     * @param minRate the minimum rate
     * @return list of jobs with rate above the minimum (latest versions only)
     */
    List<Job> findJobsWithRateAbove(Double minRate);
}