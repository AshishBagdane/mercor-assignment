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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the JobService interface
 */
@Service
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

        try {
          Path<Object> path = root.get(jpaFieldName);

          switch (operator) {
            case ">":
              if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThan(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else if (value instanceof Number) {
                // Handle different numeric types
                if (value instanceof Integer) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThan(path.as(Integer.class), (Integer) value));
                } else if (value instanceof Long) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThan(path.as(Long.class), (Long) value));
                } else if (value instanceof Double) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThan(path.as(Double.class), (Double) value));
                } else if (value instanceof BigDecimal) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThan(path.as(BigDecimal.class), (BigDecimal) value));
                } else {
                  // Default to string comparison if unknown number type
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThan(path.as(String.class), value.toString()));
                }
              } else {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThan(path.as(String.class), value.toString()));
              }
              break;
            case ">=":
              if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else if (value instanceof Number) {
                // Handle different numeric types
                if (value instanceof Integer) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThanOrEqualTo(path.as(Integer.class), (Integer) value));
                } else if (value instanceof Long) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThanOrEqualTo(path.as(Long.class), (Long) value));
                } else if (value instanceof Double) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThanOrEqualTo(path.as(Double.class), (Double) value));
                } else if (value instanceof BigDecimal) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThanOrEqualTo(path.as(BigDecimal.class), (BigDecimal) value));
                } else {
                  // Default to string comparison if unknown number type
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), value.toString()));
                }
              } else {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), value.toString()));
              }
              break;
            case "<":
              if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThan(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else if (value instanceof Number) {
                // Handle different numeric types
                if (value instanceof Integer) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThan(path.as(Integer.class), (Integer) value));
                } else if (value instanceof Long) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThan(path.as(Long.class), (Long) value));
                } else if (value instanceof Double) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThan(path.as(Double.class), (Double) value));
                } else if (value instanceof BigDecimal) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThan(path.as(BigDecimal.class), (BigDecimal) value));
                } else {
                  // Default to string comparison if unknown number type
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThan(path.as(String.class), value.toString()));
                }
              } else {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThan(path.as(String.class), value.toString()));
              }
              break;
            case "<=":
              if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else if (value instanceof Number) {
                // Handle different numeric types
                if (value instanceof Integer) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThanOrEqualTo(path.as(Integer.class), (Integer) value));
                } else if (value instanceof Long) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThanOrEqualTo(path.as(Long.class), (Long) value));
                } else if (value instanceof Double) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThanOrEqualTo(path.as(Double.class), (Double) value));
                } else if (value instanceof BigDecimal) {
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThanOrEqualTo(path.as(BigDecimal.class), (BigDecimal) value));
                } else {
                  // Default to string comparison if unknown number type
                  predicate = criteriaBuilder.and(predicate,
                      criteriaBuilder.lessThanOrEqualTo(path.as(String.class), value.toString()));
                }
              } else {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(path.as(String.class), value.toString()));
              }
              break;
            case "!=":
              if (value instanceof Boolean) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.notEqual(path, value));
              } else if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.notEqual(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.notEqual(path, value));
              }
              break;
            case "LIKE":
              predicate = criteriaBuilder.and(predicate,
                  criteriaBuilder.like(path.as(String.class), "%" + value.toString() + "%"));
              break;
            case "=":
            default:
              // Handle special cases based on field and value type
              if (jpaFieldName.equals("companyId") || jpaFieldName.equals("contractorId")
                  || jpaFieldName.equals("status")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(path, value.toString()));
              } else if (jpaFieldName.equals("rate")) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(path.as(BigDecimal.class), new BigDecimal(value.toString())));
              } else if (jpaFieldName.equals("title")) {
                // For title, use LIKE for partial matching
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.like(path.as(String.class), "%" + value.toString() + "%"));
              } else if (value instanceof Boolean) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(path, value));
              } else {
                // Default case for other fields
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(path, value));
              }
              break;
          }
        } catch (IllegalArgumentException e) {
          throw new SCDException("Invalid field name: " + fieldName, "INVALID_FIELD_NAME");
        } catch (Exception e) {
          throw new SCDException("Error creating specification for field " + fieldName + ": " + e.getMessage(),
              "SPECIFICATION_ERROR");
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