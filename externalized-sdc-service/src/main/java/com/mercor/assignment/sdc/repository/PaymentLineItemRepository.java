package com.mercor.assignment.sdc.repository;

import com.mercor.assignment.sdc.domain.entity.PaymentLineItem;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PaymentLineItem entity
 */
@Repository
public interface PaymentLineItemRepository extends SCDRepository<PaymentLineItem> {

  /**
   * Find payment line items for a specific job (latest versions only)
   *
   * @param jobUid Job UID
   * @return List of payment line items for the specified job
   */
  @Query(value = "SELECT p FROM PaymentLineItem p WHERE p.jobUid = :jobUid " +
      "AND p.version IN " +
      "(SELECT MAX(v.version) FROM PaymentLineItem v WHERE v.id = p.id GROUP BY v.id)")
  List<PaymentLineItem> findPaymentLineItemsForJob(@Param("jobUid") String jobUid);

  /**
   * Find payment line items for a specific timelog (latest versions only)
   *
   * @param timelogUid Timelog UID
   * @return List of payment line items for the specified timelog
   */
  @Query(value = "SELECT p FROM PaymentLineItem p WHERE p.timelogUid = :timelogUid " +
      "AND p.version IN " +
      "(SELECT MAX(v.version) FROM PaymentLineItem v WHERE v.id = p.id GROUP BY v.id)")
  List<PaymentLineItem> findPaymentLineItemsForTimelog(@Param("timelogUid") String timelogUid);

  /**
   * Find payment line items for a specific contractor within a date range (latest versions only)
   *
   * @param jobUids   List of job UIDs associated with the contractor
   * @param startTime Start timestamp (in milliseconds) used to filter related timelogs
   * @param endTime   End timestamp (in milliseconds) used to filter related timelogs
   * @return List of payment line items for the contractor within the specified time range
   */
  @Query(value = "SELECT p FROM PaymentLineItem p WHERE p.jobUid IN :jobUids " +
      "AND p.timelogUid IN " +
      "(SELECT t.uid FROM Timelog t WHERE t.timeStart >= :startTime AND t.timeEnd <= :endTime) " +
      "AND p.version IN " +
      "(SELECT MAX(v.version) FROM PaymentLineItem v WHERE v.id = p.id GROUP BY v.id)")
  List<PaymentLineItem> findPaymentLineItemsForContractor(
      @Param("jobUids") List<String> jobUids,
      @Param("startTime") Long startTime,
      @Param("endTime") Long endTime);

  /**
   * Calculate the total amount of payment line items for a specific contractor within a date range (latest versions only)
   *
   * @param jobUids   List of job UIDs associated with the contractor
   * @param startTime Start timestamp (in milliseconds) used to filter related timelogs
   * @param endTime   End timestamp (in milliseconds) used to filter related timelogs
   * @return Total amount of payment line items
   */
  @Query(value = "SELECT SUM(p.amount) FROM PaymentLineItem p WHERE p.jobUid IN :jobUids " +
      "AND p.timelogUid IN " +
      "(SELECT t.uid FROM Timelog t WHERE t.timeStart >= :startTime AND t.timeEnd <= :endTime) " +
      "AND p.version IN " +
      "(SELECT MAX(v.version) FROM PaymentLineItem v WHERE v.id = p.id GROUP BY v.id)")
  Double getTotalAmountForContractor(
      @Param("jobUids") List<String> jobUids,
      @Param("startTime") Long startTime,
      @Param("endTime") Long endTime);
}