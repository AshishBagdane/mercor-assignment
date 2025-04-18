package com.mercor.assignment.sdc.service.impl;

import com.mercor.assignment.sdc.domain.dto.JobDTO;
import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import com.mercor.assignment.sdc.domain.entity.Job;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.repository.JobRepository;
import com.mercor.assignment.sdc.service.JobService;
import com.mercor.assignment.sdc.service.mapper.JobMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the JobService interface
 */
@Service
@Slf4j
public class JobServiceImpl extends AbstractSCDService<JobDTO, Job> implements JobService {

  private final JobRepository jobRepository;
  private final JobMapper jobMapper;

  @Autowired
  public JobServiceImpl(JobRepository jobRepository, JobMapper jobMapper) {
    super(jobRepository, jobMapper);
    this.jobRepository = jobRepository;
    this.jobMapper = jobMapper;
  }

  @Override
  public List<JobDTO> getActiveJobsForCompany(String companyId) {
    if (companyId == null || companyId.isEmpty()) {
      throw new SCDException("Company ID cannot be null or empty", "INVALID_COMPANY_ID");
    }
    return jobRepository.findActiveJobsForCompany(companyId).stream()
        .map(jobMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<JobDTO> getActiveJobsForContractor(String contractorId) {
    if (contractorId == null || contractorId.isEmpty()) {
      throw new SCDException("Contractor ID cannot be null or empty", "INVALID_CONTRACTOR_ID");
    }
    return jobRepository.findActiveJobsForContractor(contractorId).stream()
        .map(jobMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<JobDTO> getJobsWithRateAbove(Double rate) {
    return jobRepository.findJobsWithRateAbove(rate).stream()
        .map(jobMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  public JobDTO updateStatus(String id, String status) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Job ID cannot be null or empty", "INVALID_JOB_ID");
    }

    if (status == null || status.isEmpty()) {
      throw new SCDException("Status cannot be null or empty", "INVALID_STATUS");
    }

    // Validate status - assuming valid values are "active", "completed",
    // "cancelled", etc.
    List<String> validStatuses = List.of("active", "completed", "cancelled", "extended", "on_hold");
    if (!validStatuses.contains(status.toLowerCase())) {
      throw new SCDException("Invalid status value: " + status + ". Valid values are: " +
          String.join(", ", validStatuses), "INVALID_STATUS_VALUE");
    }

    Map<String, Object> fields = new HashMap<>();
    fields.put("status", status.toLowerCase());

    SCDUpdateRequest<JobDTO> updateRequest = new SCDUpdateRequest<>();
    updateRequest.setFields(fields);

    return update(id, updateRequest);
  }

  @Override
  @Transactional
  public JobDTO updateRate(String id, Double rate) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Job ID cannot be null or empty", "INVALID_JOB_ID");
    }

    if (rate == null) {
      throw new SCDException("Rate cannot be null", "INVALID_RATE");
    }

    if (rate <= 0) {
      throw new SCDException("Rate must be greater than zero", "INVALID_RATE_VALUE");
    }

    Map<String, Object> fields = new HashMap<>();
    fields.put("rate", BigDecimal.valueOf(rate));

    SCDUpdateRequest<JobDTO> updateRequest = new SCDUpdateRequest<>();
    updateRequest.setFields(fields);

    return update(id, updateRequest);
  }

  @Override
  protected Specification<Job> createSpecification(SCDQueryRequest queryRequest) {
    return (root, query, criteriaBuilder) -> {
      // Start with a predicate that's always true
      Predicate predicate = criteriaBuilder.conjunction();

      Map<String, Object> conditions = queryRequest.getConditions();
      if (conditions == null || conditions.isEmpty()) {
        return predicate;
      }

      // Keep track of processed field names to avoid duplicate processing
      Set<String> processedFields = new HashSet<>();

      // Process boolean conditions first
      for (Map.Entry<String, Object> entry : conditions.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        if (value == null) {
          continue;
        }

        // Parse the condition key to extract field name and operator
        String[] parsedKey = SCDQueryRequest.parseConditionKey(key);
        String fieldName = parsedKey[0];
        String operator = parsedKey[1];

        // Convert snake_case field names to camelCase for JPA
        String jpaFieldName = SCDQueryRequest.toCamelCase(fieldName);

        // Always convert boolean strings to actual Boolean objects
        if (value instanceof String &&
            ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
          value = Boolean.parseBoolean((String) value);
        }

        if (value instanceof Boolean) {
          try {
            Path<Boolean> path = root.get(jpaFieldName);
            boolean boolValue = (Boolean) value;

            if ("!=".equals(operator)) {
              predicate = criteriaBuilder.and(predicate, criteriaBuilder.notEqual(path, boolValue));
            } else {
              predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(path, boolValue));
            }

            // Mark this field as processed
            processedFields.add(jpaFieldName);
          } catch (Exception e) {
            throw new SCDException("Error processing boolean field " + fieldName + ": " + e.getMessage(),
                "SPECIFICATION_ERROR");
          }
        }
      }

      // Process special case fields with strongly-typed handling
      // Rate comparisons
      if (conditions.containsKey("rate") && !processedFields.contains("rate")) {
        Object value = conditions.get("rate");
        BigDecimal numericValue;

        if (value instanceof BigDecimal) {
          numericValue = (BigDecimal) value;
        } else if (value instanceof Number) {
          numericValue = BigDecimal.valueOf(((Number) value).doubleValue());
        } else {
          numericValue = new BigDecimal(value.toString());
        }

        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("rate"), numericValue));
        processedFields.add("rate");
      } else if (conditions.containsKey("minRate") && !processedFields.contains("rate")) {
        Object value = conditions.get("minRate");
        BigDecimal numericValue;

        if (value instanceof BigDecimal) {
          numericValue = (BigDecimal) value;
        } else if (value instanceof Number) {
          numericValue = BigDecimal.valueOf(((Number) value).doubleValue());
        } else {
          numericValue = new BigDecimal(value.toString());
        }

        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.greaterThanOrEqualTo(root.get("rate"), numericValue));
        processedFields.add("rate");
      } else if (conditions.containsKey("maxRate") && !processedFields.contains("rate")) {
        Object value = conditions.get("maxRate");
        BigDecimal numericValue;

        if (value instanceof BigDecimal) {
          numericValue = (BigDecimal) value;
        } else if (value instanceof Number) {
          numericValue = BigDecimal.valueOf(((Number) value).doubleValue());
        } else {
          numericValue = new BigDecimal(value.toString());
        }

        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.lessThanOrEqualTo(root.get("rate"), numericValue));
        processedFields.add("rate");
      }

      // Company ID handling
      if (conditions.containsKey("companyId") && !processedFields.contains("companyId")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("companyId"), conditions.get("companyId").toString()));
        processedFields.add("companyId");
      }

      // Contractor ID handling
      if (conditions.containsKey("contractorId") && !processedFields.contains("contractorId")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("contractorId"), conditions.get("contractorId").toString()));
        processedFields.add("contractorId");
      }

      // Status handling
      if (conditions.containsKey("status") && !processedFields.contains("status")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("status"), conditions.get("status").toString()));
        processedFields.add("status");
      }

      // Title handling - uses LIKE for partial matching
      if (conditions.containsKey("title") && !processedFields.contains("title")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.like(root.get("title").as(String.class),
                "%" + conditions.get("title").toString() + "%"));
        processedFields.add("title");
      }

      // Process any remaining fields not handled above with generic handling
      for (Map.Entry<String, Object> entry : conditions.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        // Skip null values and already processed fields
        if (value == null) {
          continue;
        }

        // Parse the condition key to extract field name and operator
        String[] parsedKey = SCDQueryRequest.parseConditionKey(key);
        String fieldName = parsedKey[0];
        String operator = parsedKey[1];

        // Convert snake_case field names to camelCase for JPA
        String jpaFieldName = SCDQueryRequest.toCamelCase(fieldName);

        // Skip already processed fields
        if (processedFields.contains(jpaFieldName)) {
          continue;
        }

        // Handle any remaining conditions
        try {
          Path<Object> path = root.get(jpaFieldName);

          switch (operator) {
            case ">":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.greaterThan(path.as(String.class), value.toString()));
              break;
            case ">=":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), value.toString()));
              break;
            case "<":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.lessThan(path.as(String.class), value.toString()));
              break;
            case "<=":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.lessThanOrEqualTo(path.as(String.class), value.toString()));
              break;
            case "!=":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.notEqual(path, value));
              break;
            case "LIKE":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.like(path.as(String.class), "%" + value.toString() + "%"));
              break;
            case "=":
            default:
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.equal(path, value));
              break;
          }
        } catch (Exception e) {
          log.warn("Error processing field {}: {}", fieldName, e.getMessage());
        }
      }

      return predicate;
    };
  }

  @Override
  protected void updateFields(Job entity, Map<String, Object> fields) {
    if (fields.containsKey("status")) {
      entity.setStatus((String) fields.get("status"));
    }

    if (fields.containsKey("rate")) {
      if (fields.get("rate") instanceof BigDecimal) {
        entity.setRate((BigDecimal) fields.get("rate"));
      } else if (fields.get("rate") instanceof Double) {
        entity.setRate(BigDecimal.valueOf((Double) fields.get("rate")));
      }
    }

    if (fields.containsKey("title")) {
      entity.setTitle((String) fields.get("title"));
    }
  }

  @Override
  protected void copyProperties(Job source, Job target) {
    target.setStatus(source.getStatus());
    target.setRate(source.getRate());
    target.setTitle(source.getTitle());
    target.setCompanyId(source.getCompanyId());
    target.setContractorId(source.getContractorId());
  }
}