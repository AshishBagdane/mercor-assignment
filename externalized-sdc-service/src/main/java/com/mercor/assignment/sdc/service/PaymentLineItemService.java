package com.mercor.assignment.sdc.service;

import com.mercor.assignment.sdc.domain.dto.PaymentLineItemDTO;
import java.util.List;

/**
 * Service interface for PaymentLineItem entity operations
 */
public interface PaymentLineItemService extends SCDService<PaymentLineItemDTO> {

  /**
   * Get payment line items for a specific job
   *
   * @param jobUid Job UID
   * @return List of payment line items for the specified job (latest versions only)
   */
  List<PaymentLineItemDTO> getPaymentLineItemsForJob(String jobUid);

  /**
   * Get payment line items for a specific timelog
   *
   * @param timelogUid Timelog UID
   * @return List of payment line items for the specified timelog (latest versions only)
   */
  List<PaymentLineItemDTO> getPaymentLineItemsForTimelog(String timelogUid);

  /**
   * Get payment line items for a specific contractor within a date range
   *
   * @param contractorId Contractor ID
   * @param startTime    Start timestamp (in milliseconds)
   * @param endTime      End timestamp (in milliseconds)
   * @return List of payment line items for the contractor within the specified time range (latest versions only)
   */
  List<PaymentLineItemDTO> getPaymentLineItemsForContractor(String contractorId, Long startTime, Long endTime);

  /**
   * Mark a payment line item as paid, creating a new version
   *
   * @param id Payment line item ID
   * @return The newly created payment line item version with paid status
   */
  PaymentLineItemDTO markAsPaid(String id);

  /**
   * Get the total amount of payment line items for a specific contractor within a date range
   *
   * @param contractorId Contractor ID
   * @param startTime    Start timestamp (in milliseconds)
   * @param endTime      End timestamp (in milliseconds)
   * @return Total amount of payment line items
   */
  Double getTotalAmountForContractor(String contractorId, Long startTime, Long endTime);
}