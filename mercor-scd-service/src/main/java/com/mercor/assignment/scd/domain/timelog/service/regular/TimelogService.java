package com.mercor.assignment.scd.domain.timelog.service.regular;

import com.mercor.assignment.scd.domain.core.service.SCDService;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;

import java.util.List;

/**
 * Service interface for Timelog-specific operations
 * Extends the generic SCDService with Timelog-specific methods
 */
public interface TimelogService extends SCDService<Timelog> {

    /**
     * Find all timelogs for a specific job
     *
     * @param jobUid the job UID
     * @return list of timelogs for the job (latest versions only)
     */
    List<Timelog> findTimelogsForJob(String jobUid);

    /**
     * Find all timelogs for a specific contractor within a time range
     *
     * @param contractorId the contractor ID
     * @param startTime the start time in milliseconds
     * @param endTime the end time in milliseconds
     * @return list of timelogs for the contractor within the time range (latest versions only)
     */
    List<Timelog> findTimelogsForContractor(String contractorId, Long startTime, Long endTime);

    /**
     * Find timelogs with duration above the specified minimum
     *
     * @param minDuration the minimum duration in milliseconds
     * @return list of timelogs with duration above the minimum (latest versions only)
     */
    List<Timelog> findTimelogsWithDurationAbove(Long minDuration);

    /**
     * Adjust a timelog by creating a new version with adjusted duration
     *
     * @param timelogId the ID of the timelog to adjust
     * @param adjustedDuration the new adjusted duration
     * @return the newly created timelog version with adjusted duration
     */
    Timelog adjustTimelog(String timelogId, Long adjustedDuration);
}