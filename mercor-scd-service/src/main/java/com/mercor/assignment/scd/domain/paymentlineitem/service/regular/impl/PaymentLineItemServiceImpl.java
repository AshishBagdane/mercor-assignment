package com.mercor.assignment.scd.domain.paymentlineitem.service.regular.impl;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.common.errorhandling.exceptions.ValidationException;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.service.regular.AbstractSCDServiceImpl;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.PaymentLineItemValidators;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.SCDCommonValidators;
import com.mercor.assignment.scd.domain.core.validation.SCDValidators.TimelogValidators;
import com.mercor.assignment.scd.domain.paymentlineitem.enums.PaymentLineItemStatus;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitem.repository.PaymentLineItemRepository;
import com.mercor.assignment.scd.domain.paymentlineitem.service.regular.PaymentLineItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@Slf4j
@Service(ServiceName.PAYMENT_LINE_ITEMS_SERVICE)
@Transactional(readOnly = true)
public class PaymentLineItemServiceImpl extends AbstractSCDServiceImpl<PaymentLineItem, PaymentLineItemRepository> implements PaymentLineItemService {

    private final PaymentLineItemRepository paymentLineItemRepository;

    @Autowired
    public PaymentLineItemServiceImpl(
        final PaymentLineItemRepository paymentLineItemRepository,
        final UidGenerator uidGenerator
    ) {
        super(paymentLineItemRepository, uidGenerator, EntityType.TIMELOG);
        this.paymentLineItemRepository = paymentLineItemRepository;
    }

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
    @Transactional
    public PaymentLineItem markAsPaid(String id) {
        if (!SCDCommonValidators.validId.isValid(id)) {
            throw new ValidationException("Invalid Payment Line Item ID format");
        }

        final PaymentLineItem paymentLineItem = paymentLineItemRepository.findLatestVersionById(id)
            .orElseThrow(() -> new EntityNotFoundException("Payment line item not found with ID: " + id));

        final PaymentLineItemStatus currentStatus = PaymentLineItemStatus.fromValue(paymentLineItem.getStatus());

        // Check if the payment line item is already paid
        if (PaymentLineItemStatus.PAID.equals(currentStatus)) {
            throw new ValidationException("Payment line item is already marked as paid", "ALREADY_PAID");
        }

        // Create a map with the updated status
        final Map<String, Object> fieldsToUpdate = new HashMap<>();
        fieldsToUpdate.put("status", "paid");

        return paymentLineItemRepository.createNewVersion(paymentLineItem, fieldsToUpdate);
    }

    @Override
    @Cacheable(value = "payment:totalForContractor",
        key = "#contractorId + ':' + #startTime + ':' + #endTime")
    public BigDecimal getTotalAmountForContractor(String contractorId, Long startTime, Long endTime) {
        final List<PaymentLineItem> paymentLineItems = getPaymentLineItemsForContractor(contractorId, startTime,
                                                                                        endTime);
        return paymentLineItems.stream()
            .map(PaymentLineItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Cacheable(value = "payment_line_item:latest", key = "#id", unless = "#result == null")
    public Optional<PaymentLineItem> findLatestVersionById(String id) {
        return super.findLatestVersionById(id);
    }

    @Override
    @Cacheable(value = "payment_line_item:history", key = "#id", unless = "#result == null")
    public List<PaymentLineItem> findAllVersionsById(String id) {
        return super.findAllVersionsById(id);
    }

    @Override
    public Optional<PaymentLineItem> findByUid(String uid) {
        return super.findByUid(uid);
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
        return super.createNewVersion(id, fieldsToUpdate);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "payment_line_item:totalForContractor", allEntries = true)
    })
    @Transactional
    public PaymentLineItem createEntity(PaymentLineItem entity) {
        if (!PaymentLineItemValidators.validNewPaymentLineItem.isValid(entity)) {
            throw new ValidationException("Invalid payment line item entity");
        }
        return super.createEntity(entity);
    }

    @Override
    public List<PaymentLineItem> findLatestVersionsByCriteria(Map<String, Object> criteria) {
        return super.findLatestVersionsByCriteria(criteria);
    }

    @Override
    protected void validateEntity(final PaymentLineItem entity) {
        if (!PaymentLineItemValidators.validPaymentLineItem.isValid(entity)) {
            throw new ValidationException("Invalid payment line item entity");
        }
    }
}
