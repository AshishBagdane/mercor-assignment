package com.mercor.assignment.scd.domain.job.service.regular;

import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.service.regular.AbstractSCDServiceImpl;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.JobValidators;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.repository.JobRepository;
import com.mercor.assignment.scd.domain.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the JobService interface
 * Uses the JobRepository for SCD operations
 */
@Service(ServiceName.JOB_SERVICE)
@Transactional(readOnly = true)
public class JobServiceImpl extends AbstractSCDServiceImpl<Job, JobRepository> implements JobService {

    private final JobRepository jobRepository;

    @Autowired
    public JobServiceImpl(final JobRepository jobRepository, final UidGenerator uidGenerator) {
        super(jobRepository, uidGenerator, EntityType.JOBS);
        this.jobRepository = jobRepository;
    }

    @Override
    @Cacheable(value = "job:latest", key = "#id", unless = "#result == null")
    public Optional<Job> findLatestVersionById(String id) {
        return super.findLatestVersionById(id);
    }

    @Override
    @Cacheable(value = "job:history", key = "#id", unless = "#result == null")
    public List<Job> findAllVersionsById(String id) {
        return super.findAllVersionsById(id);
    }

    @Override
    public Optional<Job> findByUid(String uid) {
        return super.findByUid(uid);
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
        return super.createNewVersion(id, fieldsToUpdate);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "job:activeByCompany", allEntries = true),
        @CacheEvict(value = "job:activeByContractor", allEntries = true)
    })
    @Transactional
    public Job createEntity(Job entity) {
        if (!JobValidators.validNewJob.isValid(entity)) {
            throw new ValidationException("Invalid job entity");
        }
        return super.createEntity(entity);
    }

    @Override
    public List<Job> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return super.findLatestVersionsByCriteria(criteria);
    }

    @Override
    protected void validateEntity(final Job entity) {
        if (!SCDValidators.JobValidators.validJob.isValid(entity)) {
            throw new ValidationException("Invalid job entity");
        }
    }

    @Override
    @Cacheable(value = "job:activeByCompany", key = "#companyId", unless = "#result.isEmpty()")
    public List<Job> findActiveJobsForCompany(String companyId) {
        if (!SCDValidators.JobValidators.validCompanyId.isValid(companyId)) {
            throw new ValidationException("Invalid company ID format");
        }

        final Map<String, Object> criteria = new HashMap<>();
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

        final Map<String, Object> criteria = new HashMap<>();
        criteria.put("contractorId", contractorId);
        criteria.put("status", "active");

        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    public List<Job> findJobsWithRateAbove(Double minRate) {
        if (minRate == null || minRate < 0) {
            throw new IllegalArgumentException("Minimum rate must be a non-negative value");
        }

        final Map<String, Object> emptyCriteria = new HashMap<>();
        final List<Job> allLatestJobs = jobRepository.findLatestVersionsByCriteria(emptyCriteria);

        final BigDecimal minRateDecimal = new BigDecimal(minRate.toString());

        // Filter in memory (not ideal for large datasets)
        return allLatestJobs.stream()
            .filter(job -> job.getRate().compareTo(minRateDecimal) > 0)
            .toList();
    }
}
