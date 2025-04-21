package com.mercor.assignment.scd.domain.timelog.service.regular.impl;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.SCDCommonValidators;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.service.JobService;
import com.mercor.assignment.scd.domain.timelog.enums.TimelogType;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import com.mercor.assignment.scd.domain.timelog.repository.TimelogRepository;
import com.mercor.assignment.scd.domain.timelog.service.regular.TimelogService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Timelog service
 */
@Service(ServiceName.TIMELOG_SERVICE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelogServiceImpl implements TimelogService {

    private final JobService jobService;
    private final TimelogRepository timelogRepository;
    private final UidGenerator uidGenerator;

    @Override
    public List<Timelog> findTimelogsForJob(String jobUid) {
        if (!SCDCommonValidators.validUid.isValid(jobUid)) {
            throw new ValidationException("Invalid Job UID format");
        }
        return timelogRepository.findByJobUid(jobUid);
    }

    @Override
    public List<Timelog> findTimelogsForContractor(String contractorId, Long startTime, Long endTime) {
        final List<Job> jobs = jobService.findActiveJobsForContractor(contractorId);
        final List<String> jobUids = jobs.stream()
            .map(Job::getUid)
            .toList();

        if (jobUids.isEmpty()) {
            return List.of();
        }

        return timelogRepository.findByJobUidInAndTimeStartGreaterThanEqualAndTimeEndLessThanEqual(jobUids, startTime, endTime);
    }

    @Override
    public List<Timelog> findTimelogsWithDurationAbove(Long minDuration) {
        return timelogRepository.findByDurationGreaterThan(minDuration);
    }

    @Override
    @CacheEvict(value = "timelog:latest", key = "#timelogId")
    @Caching(evict = {
        @CacheEvict(value = "timelog:latest", key = "#timelogId"),
        @CacheEvict(value = "timelog:history", key = "#timelogId")
    })
    @Transactional
    public Timelog adjustTimelog(String timelogId, Long adjustedDuration) {
        if (!SCDCommonValidators.validId.isValid(timelogId)) {
            throw new ValidationException("Invalid Timelog ID format");
        }
        Optional<Timelog> latestVersionOpt = timelogRepository.findLatestVersionById(timelogId);

        if (latestVersionOpt.isEmpty()) {
            throw new EntityNotFoundException("Timelog with ID " + timelogId + " not found");
        }

        final Timelog latestVersion = latestVersionOpt.get();

        final TimelogType type = TimelogType.fromValue(latestVersion.getType());

        // Validate that we're not trying to adjust an already adjusted timelog
        if (TimelogType.ADJUSTED.equals(type)) {
            throw new ValidationException("Cannot adjust an already adjusted timelog", "ALREADY_ADJUSTED");
        }

        // Calculate the new timeEnd based on the timeStart and adjusted duration
        final Long newTimeEnd = latestVersion.getTimeStart() + adjustedDuration;

        // Create a new version with the adjusted duration, timeEnd and type
        final Map<String, Object> fieldsToUpdate = new HashMap<>();
        fieldsToUpdate.put("duration", adjustedDuration);
        fieldsToUpdate.put("timeEnd", newTimeEnd);
        fieldsToUpdate.put("type", "adjusted");

        return timelogRepository.createNewVersion(latestVersion, fieldsToUpdate);
    }

    @Override
    @Cacheable(value = "timelog:latest", key = "#id", unless = "#result == null")
    public Optional<Timelog> findLatestVersionById(String id) {
        if (!SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid Timelog ID format");
        }

        return timelogRepository.findLatestVersionById(id);
    }

    @Override
    @Cacheable(value = "timelog:history", key = "#id", unless = "#result == null")
    public List<Timelog> findAllVersionsById(String id) {
        if (!SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid Timelog ID format");
        }

        return timelogRepository.findAllVersionsById(id);
    }

    @Override
    public Optional<Timelog> findByUid(String uid) {
        if (!SCDCommonValidators.validUid.isValid(uid)) {
            throw new ValidationException("Invalid Timelog UID format");
        }

        return timelogRepository.findByUid(uid);
    }

    @Override
    @CacheEvict(value = "timelog:latest", key = "#timelogId")
    @Caching(evict = {
        @CacheEvict(value = "timelog:latest", key = "#timelogId"),
        @CacheEvict(value = "timelog:history", key = "#timelogId")
    })
    @Transactional
    public Timelog createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
        if (!SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid Timelog ID format");
        }

        Optional<Timelog> latestVersionOpt = timelogRepository.findLatestVersionById(id);

        if (latestVersionOpt.isEmpty()) {
            throw new EntityNotFoundException("Timelog with ID " + id + " not found");
        }

        final Timelog latestVersion = latestVersionOpt.get();
        return timelogRepository.createNewVersion(latestVersion, fieldsToUpdate);
    }

    @Override
    @Transactional
    public Timelog createEntity(Timelog entity) {
        // Generate a new entity ID if not set
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(uidGenerator.generateEntityId(EntityType.JOBS.getPrefix()));
        }

        // Set initial version
        entity.setVersion(1);

        // Generate UID for this version
        entity.setUid(uidGenerator.generateUid(EntityType.JOBS.getPrefix()));

        // Set timestamps
        final Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Explicitly use SCDRepositoryBase.save
        return timelogRepository.save(entity);
    }

    @Override
    public List<Timelog> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return timelogRepository.findLatestVersionsByCriteria(criteria);
    }
}