package com.mercor.assignment.sdc.repository;

import com.mercor.assignment.sdc.domain.entity.Timelog;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Timelog entity
 */
@Repository
public interface TimelogRepository extends SCDRepository<Timelog> {

  /**
   * Find timelogs for a specific job (latest versions only)
   *
   * @param jobUid Job UID
   * @return List of timelogs for the specified job
   */
  @Query(value = "SELECT t FROM Timelog t WHERE t.jobUid = :jobUid " +
      "AND t.version IN " +
      "(SELECT MAX(v.version) FROM Timelog v WHERE v.id = t.id GROUP BY v.id)")
  List<Timelog> findTimelogsForJob(@Param("jobUid") String jobUid);

  /**
   * Find timelogs for a specific contractor within a date range (latest versions only)
   *
   * @param jobUids   List of job UIDs associated with the contractor
   * @param startTime Start timestamp (in milliseconds)
   * @param endTime   End timestamp (in milliseconds)
   * @return List of timelogs for the contractor within the specified time range
   */
  @Query(value = "SELECT t FROM Timelog t WHERE t.jobUid IN :jobUids " +
      "AND t.timeStart >= :startTime AND t.timeEnd <= :endTime " +
      "AND t.version IN " +
      "(SELECT MAX(v.version) FROM Timelog v WHERE v.id = t.id GROUP BY v.id)")
  List<Timelog> findTimelogsForContractor(
      @Param("jobUids") List<String> jobUids,
      @Param("startTime") Long startTime,
      @Param("endTime") Long endTime);

  /**
   * Find timelogs with duration above the specified value (latest versions only)
   *
   * @param duration Minimum duration in milliseconds
   * @return List of timelogs with duration above the specified value
   */
  @Query(value = "SELECT t FROM Timelog t WHERE t.duration > :duration " +
      "AND t.version IN " +
      "(SELECT MAX(v.version) FROM Timelog v WHERE v.id = t.id GROUP BY v.id)")
  List<Timelog> findTimelogsWithDurationAbove(@Param("duration") Long duration);
}