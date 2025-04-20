package com.mercor.assignment.scd.domain.job.service.regular;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.repository.JobRepository;
import com.mercor.assignment.scd.domain.job.service.JobService;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UidGenerator uidGenerator;

    @Override
    public Optional<Job> findLatestVersionById(String id) {
        return jobRepository.findLatestVersionById(id);
    }

    @Override
    public List<Job> findAllVersionsById(String id) {
        return jobRepository.findAllVersionsById(id);
    }

    @Override
    public Optional<Job> findByUid(String uid) {
        return jobRepository.findByUid(uid);
    }

    @Override
    @Transactional
    public Job createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
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
    public List<Job> findActiveJobsForCompany(String companyId) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("companyId", companyId);
        criteria.put("status", "active");

        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    public List<Job> findActiveJobsForContractor(String contractorId) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contractorId", contractorId);
        criteria.put("status", "active");

        return jobRepository.findLatestVersionsByCriteria(criteria);
    }

    @Override
    public List<Job> findJobsWithRateAbove(Double minRate) {
        // For queries with operators other than equals,
        // we need more sophisticated criteria handling
        // This would typically use a Specification or custom query

        Map<String, Object> emptyCriteria = new HashMap<>();
        List<Job> allLatestJobs = jobRepository.findLatestVersionsByCriteria(emptyCriteria);

        BigDecimal minRateDecimal = new BigDecimal(minRate.toString());

        // Filter in memory (not ideal for large datasets)
        return allLatestJobs.stream()
            .filter(job -> job.getRate().compareTo(minRateDecimal) > 0)
            .toList();

        // A better implementation would use a custom query method in the repository
    }
}