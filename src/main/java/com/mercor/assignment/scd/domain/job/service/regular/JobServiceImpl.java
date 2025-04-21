package com.mercor.assignment.scd.domain.job.service.regular;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.repository.JobRepository;
import com.mercor.assignment.scd.domain.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the JobService interface
 * Uses the JobRepository for SCD operations
 */
@Service(ServiceName.JOB_SERVICE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UidGenerator uidGenerator;

    @Override
    @Cacheable(value = "job:latest", key = "#id", unless = "#result == null")
    public Optional<Job> findLatestVersionById(String id) {
        if (!SCDValidators.SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid job ID format");
        }
        return jobRepository.findLatestVersionById(id);
    }

    @Override
    @Cacheable(value = "job:history", key = "#id", unless = "#result == null")
    public List<Job> findAllVersionsById(String id) {
        if (!SCDValidators.SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid job ID format");
        }
        return jobRepository.findAllVersionsById(id);
    }

    @Override
    public Optional<Job> findByUid(String uid) {
        if (!SCDValidators.SCDCommonValidators.validUid.isValid(uid)) {
            throw new ValidationException("Invalid job UID format");
        }
        return jobRepository.findByUid(uid);
    }

    @Override
    @CacheEvict(value = "job:latest", key = "#id")
    @Caching(evict = {
        @CacheEvict(value = "job:latest", key = "#id"),
        @CacheEvict(value = "job:history", key = "#id"),
        @CacheEvict(value = "job:activeByCompany", allEntries = true),
        @CacheEvict(value = "job:activeByContractor", allEntries = true)
    })
    @Transactional
    public Job createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
        if (!SCDValidators.SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid job ID format");
        }

        Optional<Job> latestVersionOpt = findLatestVersionById(id);

        if (latestVersionOpt.isEmpty()) {
            throw new EntityNotFoundException("Job with ID " + id + " not found");
        }

        Job latestVersion = latestVersionOpt.get();
        return jobRepository.createNewVersion(latestVersion, fieldsToUpdate);
    }

    @Override
    @Transactional
    public Job createEntity(Job entity) {
        // Generate a new entity ID if not set
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(uidGenerator.generateEntityId(EntityType.JOBS.getPrefix()));
        }

        // Set initial version
        entity.setVersion(1);

        // Generate UID for this version
        entity.setUid(uidGenerator.generateUid(EntityType.JOBS.getPrefix()));

        // Set timestamps
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Explicitly use SCDRepositoryBase.save
        return jobRepository.save(entity);
    }

    @Override
    public List<Job> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    @Cacheable(value = "job:activeByCompany", key = "#companyId", unless = "#result.isEmpty()")
    public List<Job> findActiveJobsForCompany(String companyId) {
        if (!SCDValidators.JobValidators.validCompanyId.isValid(companyId)) {
            throw new ValidationException("Invalid company ID format");
        }

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("companyId", companyId);
        criteria.put("status", "active");

        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    @Cacheable(value = "job:activeByContractor", key = "#contractorId", unless = "#result.isEmpty()")
    public List<Job> findActiveJobsForContractor(String contractorId) {
        if (!SCDValidators.JobValidators.validContractorId.isValid(contractorId)) {
            throw new IllegalArgumentException("Invalid contractor ID format");
        }

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contractorId", contractorId);
        criteria.put("status", "active");

        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    public List<Job> findJobsWithRateAbove(Double minRate) {
        if (minRate == null || minRate < 0) {
            throw new IllegalArgumentException("Minimum rate must be a non-negative value");
        }

        Map<String, Object> emptyCriteria = new HashMap<>();
        List<Job> allLatestJobs = jobRepository.findLatestVersionsByCriteria(emptyCriteria);

        BigDecimal minRateDecimal = new BigDecimal(minRate.toString());

        // Filter in memory (not ideal for large datasets)
        return allLatestJobs.stream()
            .filter(job -> job.getRate().compareTo(minRateDecimal) > 0)
            .toList();
    }
}