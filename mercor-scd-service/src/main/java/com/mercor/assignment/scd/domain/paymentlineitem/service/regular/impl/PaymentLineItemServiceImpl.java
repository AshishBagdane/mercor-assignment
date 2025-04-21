package com.mercor.assignment.scd.domain.paymentlineitem.service.regular.impl;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.SCDCommonValidators;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitem.repository.PaymentLineItemRepository;
import com.mercor.assignment.scd.domain.paymentlineitem.service.regular.PaymentLineItemService;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service(ServiceName.PAYMENT_LINE_ITEMS_SERVICE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentLineItemServiceImpl implements PaymentLineItemService {

  private final PaymentLineItemRepository paymentLineItemRepository;
  private final UidGenerator uidGenerator;

  @Override
  public List<PaymentLineItem> getPaymentLineItemsForJob(String jobUid) {
    if (!SCDCommonValidators.validUid.isValid(jobUid)) {
      throw new ValidationException("Invalid Job UID format");
    }
    return paymentLineItemRepository.findByJobUid(jobUid);
  }

  @Override
  public List<PaymentLineItem> getPaymentLineItemsForTimelog(String timelogUid) {
    if (!SCDCommonValidators.validUid.isValid(timelogUid)) {
      throw new ValidationException("Invalid Timelog UID format");
    }
    return paymentLineItemRepository.findByTimelogUid(timelogUid);
  }

  @Override
  public List<PaymentLineItem> getPaymentLineItemsForContractor(String contractorId, Long startTime, Long endTime) {
    return paymentLineItemRepository.findAllForContractor(contractorId, startTime, endTime);
  }

  @Override
  @CacheEvict(value = "payment_line_item:latest", key = "#id")
  @Caching(evict = {
      @CacheEvict(value = "payment_line_item:latest", key = "#id"),
      @CacheEvict(value = "payment_line_item:history", key = "#id"),
      @CacheEvict(value = "payment_line_item:totalForContractor", allEntries = true)
  })
  public PaymentLineItem markAsPaid(String id) {
    if (!SCDCommonValidators.validId.isValid(id)) {
      throw new ValidationException("Invalid Payment Line Item ID format");
    }

    final PaymentLineItem paymentLineItem = paymentLineItemRepository.findLatestVersionById(id)
        .orElseThrow(() -> new EntityNotFoundException("Payment line item not found with ID: " + id));

    // Check if the payment line item is already paid
    if ("paid".equals(paymentLineItem.getStatus())) {
      throw new ValidationException("Payment line item is already marked as paid", "ALREADY_PAID");
    }

    // Create a map with the updated status
    Map<String, Object> fieldsToUpdate = new HashMap<>();
    fieldsToUpdate.put("status", "paid");

    return paymentLineItemRepository.createNewVersion(paymentLineItem, fieldsToUpdate);
  }

  @Override
  @Cacheable(value = "payment:totalForContractor",
      key = "#contractorId + ':' + #startTime + ':' + #endTime")
  public BigDecimal getTotalAmountForContractor(String contractorId, Long startTime, Long endTime) {
    List<PaymentLineItem> paymentLineItems = getPaymentLineItemsForContractor(contractorId, startTime, endTime);
    return paymentLineItems.stream()
        .map(PaymentLineItem::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Override
  @Cacheable(value = "payment_line_item:latest", key = "#id", unless = "#result == null")
  public Optional<PaymentLineItem> findLatestVersionById(String id) {
    if (!SCDCommonValidators.validId.isValid(id)) {
      throw new ValidationException("Invalid Payment Line Item ID format");
    }

    return paymentLineItemRepository.findLatestVersionById(id);
  }

  @Override
  @Cacheable(value = "payment_line_item:history", key = "#id", unless = "#result == null")
  public List<PaymentLineItem> findAllVersionsById(String id) {
    if (!SCDCommonValidators.validId.isValid(id)) {
      throw new ValidationException("Invalid Payment Line Item ID format");
    }

    return paymentLineItemRepository.findAllVersionsById(id);
  }

  @Override
  public Optional<PaymentLineItem> findByUid(String uid) {
    if (!SCDCommonValidators.validUid.isValid(uid)) {
      throw new ValidationException("Invalid Payment Line Item UID format");
    }
    return paymentLineItemRepository.findByUid(uid);
  }

  @Override
  @CacheEvict(value = "payment_line_item:latest", key = "#id")
  @Caching(evict = {
      @CacheEvict(value = "payment_line_item:latest", key = "#id"),
      @CacheEvict(value = "payment_line_item:history", key = "#id"),
      @CacheEvict(value = "payment_line_item:totalForContractor", allEntries = true)
  })
  @Transactional
  public PaymentLineItem createNewVersion(String id, Map<String, Object> fieldsToUpdate) {
    if (!SCDCommonValidators.validId.isValid(id)) {
      throw new ValidationException("Invalid Payment Line Item ID format");
    }

    Optional<PaymentLineItem> latestVersionOpt = findLatestVersionById(id);
    if (latestVersionOpt.isEmpty()) {
      throw new EntityNotFoundException("Job with ID " + id + " not found");
    }

    PaymentLineItem latestVersion = latestVersionOpt.get();
    return paymentLineItemRepository.createNewVersion(latestVersion, fieldsToUpdate);
  }

  @Override
  @Transactional
  public PaymentLineItem createEntity(PaymentLineItem entity) {
    // Generate a new entity ID if not set
    if (entity.getId() == null || entity.getId().isEmpty()) {
      entity.setId(uidGenerator.generateEntityId(EntityType.PAYMENT_LINE_ITEMS.getPrefix()));
    }

    // Set initial version
    entity.setVersion(1);

    // Generate UID for this version
    entity.setUid(uidGenerator.generateUid(EntityType.PAYMENT_LINE_ITEMS.getPrefix()));

    // Set timestamps
    Date now = new Date();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    // Explicitly use SCDRepositoryBase.save
    return paymentLineItemRepository.save(entity);
  }

  @Override
  public List<PaymentLineItem> findLatestVersionsByCriteria(Map<String, Object> criteria) {
    return paymentLineItemRepository.findLatestVersionsByCriteria(criteria);
  }
}
