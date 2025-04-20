package com.mercor.assignment.scd.domain.paymentlineitem.service.regular;

import com.mercor.assignment.scd.domain.core.service.SCDService;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Service interface for PaymentLineItem-specific operations
 * Extends the generic SCDService with PaymentLineItem-specific methods
 */
public interface PaymentLineItemService extends SCDService<PaymentLineItem> {

    /**
     * Find payment line items for a specific job
     *
     * @param jobUid the job UID
     * @return list of payment line items for the job (latest versions only)
     */
    List<PaymentLineItem> getPaymentLineItemsForJob(String jobUid);

    /**
     * Find payment line items for a specific timelog
     *
     * @param timelogUid the timelog UID
     * @return list of payment line items for the timelog (latest versions only)
     */
    List<PaymentLineItem> getPaymentLineItemsForTimelog(String timelogUid);

    /**
     * Find payment line items for a specific contractor within a time period
     *
     * @param contractorId the contractor ID
     * @param startTime the start time of the period
     * @param endTime the end time of the period
     * @return list of payment line items for the contractor in the period (latest versions only)
     */
    List<PaymentLineItem> getPaymentLineItemsForContractor(String contractorId, Long startTime, Long endTime);

    /**
     * Mark a payment line item as paid
     *
     * @param id the payment line item ID
     * @return the updated payment line item (new version)
     */
    PaymentLineItem markAsPaid(String id);

    /**
     * Calculate the total amount for a specific contractor within a time period
     *
     * @param contractorId the contractor ID
     * @param startTime the start time of the period
     * @param endTime the end time of the period
     * @return the total amount
     */
    BigDecimal getTotalAmountForContractor(String contractorId, Long startTime, Long endTime);
}