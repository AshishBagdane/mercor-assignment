package com.mercor.assignment.sdc.service;

import com.mercor.assignment.sdc.domain.dto.TimelogDTO;
import java.util.List;

/**
 * Service interface for Timelog entity operations
 */
public interface TimelogService extends SCDService<TimelogDTO> {

  /**
   * Get timelogs for a specific job
   *
   * @param jobUid Job UID
   * @return List of timelogs for the specified job (latest versions only)
   */
  List<TimelogDTO> getTimelogsForJob(String jobUid);

  /**
   * Get timelogs for a specific contractor within a date range
   *
   * @param contractorId Contractor ID
   * @param startTime    Start timestamp (in milliseconds)
   * @param endTime      End timestamp (in milliseconds)
   * @return List of timelogs for the contractor within the specified time range (latest versions only)
   */
  List<TimelogDTO> getTimelogsForContractor(String contractorId, Long startTime, Long endTime);

  /**
   * Get timelogs with duration above the specified value
   *
   * @param duration Minimum duration in milliseconds
   * @return List of timelogs with duration above the specified value (latest versions only)
   */
  List<TimelogDTO> getTimelogsWithDurationAbove(Long duration);

  /**
   * Adjust a timelog, creating a new version with the adjusted type
   *
   * @param id               Timelog ID
   * @param adjustedDuration New duration value
   * @return The newly created timelog version with adjusted type
   */
  TimelogDTO adjustTimelog(String id, Long adjustedDuration);
}