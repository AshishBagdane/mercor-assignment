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
      if (conditions != null) {
        if (conditions.containsKey("jobUid")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.equal(root.get("jobUid"), conditions.get("jobUid")));
        }

        if (conditions.containsKey("timelogUid")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.equal(root.get("timelogUid"), conditions.get("timelogUid")));
        }

        if (conditions.containsKey("status")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.equal(root.get("status"), conditions.get("status")));
        }

        if (conditions.containsKey("minAmount")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.greaterThanOrEqualTo(root.get("amount"),
                  new BigDecimal(conditions.get("minAmount").toString())));
        }

        if (conditions.containsKey("maxAmount")) {
          predicate = criteriaBuilder.and(predicate,
              criteriaBuilder.lessThanOrEqualTo(root.get("amount"),
                  new BigDecimal(conditions.get("maxAmount").toString())));
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
}