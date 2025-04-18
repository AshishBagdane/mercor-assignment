package com.mercor.assignment.sdc.service.impl;

import com.mercor.assignment.sdc.domain.dto.PaymentLineItemDTO;
import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import com.mercor.assignment.sdc.domain.entity.Job;
import com.mercor.assignment.sdc.domain.entity.PaymentLineItem;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.repository.JobRepository;
import com.mercor.assignment.sdc.repository.PaymentLineItemRepository;
import com.mercor.assignment.sdc.service.PaymentLineItemService;
import com.mercor.assignment.sdc.service.mapper.PaymentLineItemMapper;
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
 * Implementation of the PaymentLineItemService interface
 */
@Service
public class PaymentLineItemServiceImpl extends AbstractSCDService<PaymentLineItemDTO, PaymentLineItem>
    implements PaymentLineItemService {

  private final PaymentLineItemRepository paymentLineItemRepository;
  private final JobRepository jobRepository;
  private final PaymentLineItemMapper paymentLineItemMapper;

  @Autowired
  public PaymentLineItemServiceImpl(PaymentLineItemRepository paymentLineItemRepository,
      JobRepository jobRepository,
      PaymentLineItemMapper paymentLineItemMapper) {
    super(paymentLineItemRepository, paymentLineItemMapper);
    this.paymentLineItemRepository = paymentLineItemRepository;
    this.jobRepository = jobRepository;
    this.paymentLineItemMapper = paymentLineItemMapper;
  }

  @Override
  public List<PaymentLineItemDTO> getPaymentLineItemsForJob(String jobUid) {
    List<PaymentLineItem> paymentLineItems = paymentLineItemRepository.findPaymentLineItemsForJob(jobUid);
    return paymentLineItems.stream()
        .map(paymentLineItemMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<PaymentLineItemDTO> getPaymentLineItemsForTimelog(String timelogUid) {
    List<PaymentLineItem> paymentLineItems = paymentLineItemRepository.findPaymentLineItemsForTimelog(timelogUid);
    return paymentLineItems.stream()
        .map(paymentLineItemMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<PaymentLineItemDTO> getPaymentLineItemsForContractor(String contractorId, Long startTime, Long endTime) {
    // First, find all job UIDs for the contractor (latest versions only)
    List<Job> jobs = jobRepository.findActiveJobsForContractor(contractorId);
    List<String> jobUids = jobs.stream()
        .map(Job::getUid)
        .collect(Collectors.toList());

    if (jobUids.isEmpty()) {
      return List.of();
    }

    // Then, find all payment line items for these jobs within the time range
    List<PaymentLineItem> paymentLineItems = paymentLineItemRepository.findPaymentLineItemsForContractor(jobUids,
        startTime, endTime);
    return paymentLineItems.stream()
        .map(paymentLineItemMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public PaymentLineItemDTO markAsPaid(String id) {
    if (id == null || id.isEmpty()) {
      throw new SCDException("Payment line item ID cannot be null or empty", "INVALID_PAYMENT_ID");
    }

    // Check if the payment line item exists
    PaymentLineItem paymentLineItem = paymentLineItemRepository.findFirstByIdOrderByVersionDesc(id)
        .orElseThrow(() -> new EntityNotFoundException("Payment line item not found with ID: " + id));

    // Check if the payment line item is already paid
    if ("paid".equals(paymentLineItem.getStatus())) {
      throw new SCDException("Payment line item is already marked as paid", "ALREADY_PAID");
    }

    // Create a map with the updated status
    Map<String, Object> fields = new HashMap<>();
    fields.put("status", "paid");

    SCDUpdateRequest<PaymentLineItemDTO> updateRequest = new SCDUpdateRequest<>();
    updateRequest.setFields(fields);

    return update(id, updateRequest);
  }

  @Override
  public Double getTotalAmountForContractor(String contractorId, Long startTime, Long endTime) {
    // First, find all job UIDs for the contractor (latest versions only)
    List<Job> jobs = jobRepository.findActiveJobsForContractor(contractorId);
    List<String> jobUids = jobs.stream()
        .map(Job::getUid)
        .collect(Collectors.toList());

    if (jobUids.isEmpty()) {
      return 0.0;
    }

    // Then, calculate the total amount of payment line items for these jobs within
    // the time range
    Double totalAmount = paymentLineItemRepository.getTotalAmountForContractor(jobUids, startTime, endTime);
    return totalAmount != null ? totalAmount : 0.0;
  }

  @Override
  protected Specification<PaymentLineItem> createSpecification(SCDQueryRequest queryRequest) {
    return (root, query, criteriaBuilder) -> {
      // Start with a predicate that's always true
      Predicate predicate = criteriaBuilder.conjunction();

      Map<String, Object> conditions = queryRequest.getConditions();
      if (conditions == null || conditions.isEmpty()) {
        return predicate;
      }

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

        if (value instanceof Boolean ||
            (value instanceof String
                && ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)))) {
          Boolean boolValue = value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString());

          try {
            Path<Boolean> path = root.get(jpaFieldName);

            if ("!=".equals(operator)) {
              predicate = criteriaBuilder.and(predicate, criteriaBuilder.notEqual(path, boolValue));
            } else {
              predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(path, boolValue));
            }
          } catch (Exception e) {
            throw new SCDException("Error processing boolean field " + fieldName + ": " + e.getMessage(),
                "SPECIFICATION_ERROR");
          }
        }
      }

      // Process non-boolean fields using more explicit typed handling
      if (conditions.containsKey("jobUid")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("jobUid"), conditions.get("jobUid").toString()));
      }

      if (conditions.containsKey("timelogUid")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("timelogUid"), conditions.get("timelogUid").toString()));
      }

      if (conditions.containsKey("status")) {
        predicate = criteriaBuilder.and(predicate,
            criteriaBuilder.equal(root.get("status"), conditions.get("status").toString()));
      }

      if (conditions.containsKey("minAmount")) {
        Object value = conditions.get("minAmount");
        if (value instanceof BigDecimal) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("amount").as(BigDecimal.class), (BigDecimal) value));
        } else if (value instanceof Number) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("amount").as(BigDecimal.class),
                  BigDecimal.valueOf(((Number) value).doubleValue())));
        } else {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("amount").as(BigDecimal.class),
                  new BigDecimal(value.toString())));
        }
      }

      if (conditions.containsKey("maxAmount")) {
        Object value = conditions.get("maxAmount");
        if (value instanceof BigDecimal) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("amount").as(BigDecimal.class), (BigDecimal) value));
        } else if (value instanceof Number) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("amount").as(BigDecimal.class),
                  BigDecimal.valueOf(((Number) value).doubleValue())));
        } else {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("amount").as(BigDecimal.class),
                  new BigDecimal(value.toString())));
        }
      }

      // Process any additional fields with complex conditions
      for (Map.Entry<String, Object> entry : conditions.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        // Skip already processed fields and null values
        if (value == null || key.equals("jobUid") || key.equals("timelogUid") ||
            key.equals("status") || key.equals("minAmount") || key.equals("maxAmount") ||
            (value instanceof Boolean) || (value instanceof String &&
                ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)))) {
          continue;
        }

        // Parse the condition key to extract field name and operator
        String[] parsedKey = SCDQueryRequest.parseConditionKey(key);
        String fieldName = parsedKey[0];
        String operator = parsedKey[1];

        // Handle any remaining conditions
        try {
          String jpaFieldName = SCDQueryRequest.toCamelCase(fieldName);
          Path<Object> path = root.get(jpaFieldName);

          // Special handling for amount field
          if (jpaFieldName.equals("amount")) {
            BigDecimal numericValue;
            if (value instanceof BigDecimal) {
              numericValue = (BigDecimal) value;
            } else if (value instanceof Number) {
              numericValue = BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
              numericValue = new BigDecimal(value.toString());
            }

            switch (operator) {
              case ">":
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThan(path.as(BigDecimal.class), numericValue));
                break;
              case ">=":
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(path.as(BigDecimal.class), numericValue));
                break;
              case "<":
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThan(path.as(BigDecimal.class), numericValue));
                break;
              case "<=":
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(path.as(BigDecimal.class), numericValue));
                break;
              case "!=":
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.notEqual(path.as(BigDecimal.class), numericValue));
                break;
              case "=":
              default:
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(path.as(BigDecimal.class), numericValue));
                break;
            }
          } else {
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
          }
        } catch (Exception e) {
          throw new SCDException("Error processing field " + fieldName + ": " + e.getMessage(),
              "SPECIFICATION_ERROR");
        }
      }

      return predicate;
    };
  }

  @Override
  protected void updateFields(PaymentLineItem entity, Map<String, Object> fields) {
    if (fields.containsKey("jobUid")) {
      entity.setJobUid((String) fields.get("jobUid"));
    }

    if (fields.containsKey("timelogUid")) {
      entity.setTimelogUid((String) fields.get("timelogUid"));
    }

    if (fields.containsKey("amount")) {
      if (fields.get("amount") instanceof BigDecimal) {
        entity.setAmount((BigDecimal) fields.get("amount"));
      } else if (fields.get("amount") instanceof Double) {
        entity.setAmount(BigDecimal.valueOf((Double) fields.get("amount")));
      }
    }

    if (fields.containsKey("status")) {
      entity.setStatus((String) fields.get("status"));
    }
  }

  @Override
  protected void copyProperties(PaymentLineItem source, PaymentLineItem target) {
    target.setJobUid(source.getJobUid());
    target.setTimelogUid(source.getTimelogUid());
    target.setAmount(source.getAmount());
    target.setStatus(source.getStatus());
  }

  @Override
  @Transactional
  public PaymentLineItemDTO create(PaymentLineItemDTO dto) {
    try {
      // Important: Set timelogUid to null if it's empty to avoid foreign key
      // constraint errors
      if (dto.getTimelogUid() != null && dto.getTimelogUid().isEmpty()) {
        dto.setTimelogUid(null);
      }

      return super.create(dto);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      // Handle foreign key constraints properly with user-friendly messages
      if (e.getMessage().contains("fk_payment_line_items_timelogs")) {
        throw new SCDException("The timelog reference is invalid or does not exist", e, "INVALID_TIMELOG_REFERENCE");
      } else if (e.getMessage().contains("fk_payment_line_items_jobs")) {
        throw new SCDException("The job reference is invalid or does not exist", e, "INVALID_JOB_REFERENCE");
      }
      // Re-throw other data integrity errors
      throw new SCDException("Data integrity violation during create: " + e.getMessage(), e,
          "DATA_INTEGRITY_VIOLATION");
    }
  }

  @Override
  @Transactional
  public PaymentLineItemDTO update(String id, SCDUpdateRequest<PaymentLineItemDTO> updateRequest) {
    try {
      // Handle empty timelogUid in update request entity
      if (updateRequest.getEntity() != null) {
        PaymentLineItemDTO dto = updateRequest.getEntity();
        if (dto.getTimelogUid() != null && dto.getTimelogUid().isEmpty()) {
          dto.setTimelogUid(null);
        }
      }

      // Handle empty timelogUid in fields map
      if (updateRequest.getFields() != null && updateRequest.getFields().containsKey("timelogUid")) {
        Object timelogUid = updateRequest.getFields().get("timelogUid");
        if (timelogUid instanceof String && ((String) timelogUid).isEmpty()) {
          updateRequest.getFields().put("timelogUid", null);
        }
      }

      return super.update(id, updateRequest);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      // Handle foreign key constraints properly with user-friendly messages
      if (e.getMessage().contains("fk_payment_line_items_timelogs")) {
        throw new SCDException("The timelog reference is invalid or does not exist", e, "INVALID_TIMELOG_REFERENCE");
      } else if (e.getMessage().contains("fk_payment_line_items_jobs")) {
        throw new SCDException("The job reference is invalid or does not exist", e, "INVALID_JOB_REFERENCE");
      }
      // Re-throw other data integrity errors
      throw new SCDException("Data integrity violation during update: " + e.getMessage(), e,
          "DATA_INTEGRITY_VIOLATION");
    }
  }
}