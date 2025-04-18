package com.mercor.assignment.sdc.service.impl;

import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import com.mercor.assignment.sdc.domain.dto.TimelogDTO;
import com.mercor.assignment.sdc.domain.entity.Job;
import com.mercor.assignment.sdc.domain.entity.Timelog;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.repository.JobRepository;
import com.mercor.assignment.sdc.repository.TimelogRepository;
import com.mercor.assignment.sdc.service.TimelogService;
import com.mercor.assignment.sdc.service.mapper.TimelogMapper;
import jakarta.persistence.criteria.Predicate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the TimelogService interface
 */
@Service
public class TimelogServiceImpl extends AbstractSCDService<TimelogDTO, Timelog> implements TimelogService {

  private final TimelogRepository timelogRepository;
  private final JobRepository jobRepository;
  private final TimelogMapper timelogMapper;

  @Autowired
  public TimelogServiceImpl(TimelogRepository timelogRepository,
      JobRepository jobRepository,
      TimelogMapper timelogMapper) {
    super(timelogRepository, timelogMapper);
    this.timelogRepository = timelogRepository;
    this.jobRepository = jobRepository;
    this.timelogMapper = timelogMapper;
  }

  @Override
  public List<TimelogDTO> getTimelogsForJob(String jobUid) {
    List<Timelog> timelogs = timelogRepository.findTimelogsForJob(jobUid);
    return timelogs.stream()
        .map(timelogMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<TimelogDTO> getTimelogsForContractor(String contractorId, Long startTime, Long endTime) {
    // First, find all job UIDs for the contractor (latest versions only)
    List<Job> jobs = jobRepository.findActiveJobsForContractor(contractorId);
    List<String> jobUids = jobs.stream()
        .map(Job::getUid)
        .collect(Collectors.toList());

    if (jobUids.isEmpty()) {
      return List.of();
    }

    // Then, find all timelogs for these jobs within the time range
    List<Timelog> timelogs = timelogRepository.findTimelogsForContractor(jobUids, startTime, endTime);
    return timelogs.stream()
        .map(timelogMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<TimelogDTO> getTimelogsWithDurationAbove(Long duration) {
    List<Timelog> timelogs = timelogRepository.findTimelogsWithDurationAbove(duration);
    return timelogs.stream()
        .map(timelogMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public TimelogDTO adjustTimelog(String id, Long adjustedDuration) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Timelog ID cannot be null or empty", "INVALID_TIMELOG_ID");
    }

    if (adjustedDuration == null) {
      throw new SCDException("Adjusted duration cannot be null", "INVALID_DURATION");
    }

    if (adjustedDuration <= 0) {
      throw new SCDException("Adjusted duration must be greater than zero", "INVALID_DURATION_VALUE");
    }

    // Get the latest version of the timelog
    Timelog latestTimelog = timelogRepository.findFirstByIdOrderByVersionDesc(id)
        .orElseThrow(() -> new EntityNotFoundException("Timelog not found with ID: " + id));

    // Validate that we're not trying to adjust an already adjusted timelog
    if ("adjusted".equals(latestTimelog.getType())) {
      throw new SCDException("Cannot adjust an already adjusted timelog", "ALREADY_ADJUSTED");
    }

    // Calculate the new timeEnd based on the timeStart and adjusted duration
    Long newTimeEnd = latestTimelog.getTimeStart() + adjustedDuration;

    // Create a new version with the adjusted duration, timeEnd and type
    Map<String, Object> fields = new HashMap<>();
    fields.put("duration", adjustedDuration);
    fields.put("timeEnd", newTimeEnd);
    fields.put("type", "adjusted");

    SCDUpdateRequest<TimelogDTO> updateRequest = new SCDUpdateRequest<>();
    updateRequest.setFields(fields);

    return update(id, updateRequest);
  }

  @Override
  protected Specification<Timelog> createSpecification(SCDQueryRequest queryRequest) {
    return (root, query, criteriaBuilder) -> {
      // Start with a predicate that's always true
      Predicate predicate = criteriaBuilder.conjunction();

      Map<String, Object> conditions = queryRequest.getConditions();
      if (conditions != null) {
        if (conditions.containsKey("jobUid")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.equal(root.get("jobUid"), conditions.get("jobUid")));
        }

        if (conditions.containsKey("type")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.equal(root.get("type"), conditions.get("type")));
        }

        if (conditions.containsKey("minDuration")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("duration"),
                  Long.valueOf(conditions.get("minDuration").toString())));
        }

        if (conditions.containsKey("maxDuration")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("duration"),
                  Long.valueOf(conditions.get("maxDuration").toString())));
        }

        if (conditions.containsKey("startTimeAfter")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("timeStart"),
                  Long.valueOf(conditions.get("startTimeAfter").toString())));
        }

        if (conditions.containsKey("startTimeBefore")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("timeStart"),
                  Long.valueOf(conditions.get("startTimeBefore").toString())));
        }

        if (conditions.containsKey("endTimeAfter")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("timeEnd"),
                  Long.valueOf(conditions.get("endTimeAfter").toString())));
        }

        if (conditions.containsKey("endTimeBefore")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("timeEnd"),
                  Long.valueOf(conditions.get("endTimeBefore").toString())));
        }
      }

      return predicate;
    };
  }

  @Override
  protected void updateFields(Timelog entity, Map<String, Object> fields) {
    if (fields.containsKey("duration")) {
      entity.setDuration(Long.valueOf(fields.get("duration").toString()));
    }

    if (fields.containsKey("timeStart")) {
      entity.setTimeStart(Long.valueOf(fields.get("timeStart").toString()));
    }

    if (fields.containsKey("timeEnd")) {
      entity.setTimeEnd(Long.valueOf(fields.get("timeEnd").toString()));
    }

    if (fields.containsKey("type")) {
      entity.setType((String) fields.get("type"));
    }

    if (fields.containsKey("jobUid")) {
      entity.setJobUid((String) fields.get("jobUid"));
    }
  }

  @Override
  protected void copyProperties(Timelog source, Timelog target) {
    target.setDuration(source.getDuration());
    target.setTimeStart(source.getTimeStart());
    target.setTimeEnd(source.getTimeEnd());
    target.setType(source.getType());
    target.setJobUid(source.getJobUid());
  }
}