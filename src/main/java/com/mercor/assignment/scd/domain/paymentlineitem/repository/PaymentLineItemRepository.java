package com.mercor.assignment.scd.domain.paymentlineitem.repository;

import com.mercor.assignment.scd.domain.core.repository.SCDRepositoryBase;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PaymentLineItem entity
 * Extends both JpaRepository (for standard JPA operations) and
 * SCDRepositoryBase (for SCD-specific operations)
 */
@Repository
public interface PaymentLineItemRepository extends JpaRepository<PaymentLineItem, String>,
    JpaSpecificationExecutor<PaymentLineItem>,
    SCDRepositoryBase<PaymentLineItem> {

  List<PaymentLineItem> findByJobUid(String jobUid);

  List<PaymentLineItem> findByTimelogUid(String timelogUid);

  @Query(nativeQuery = true, value =
      "SELECT pli.* FROM payment_line_items pli " +
          "JOIN (SELECT id, MAX(version) as max_version FROM payment_line_items GROUP BY id) latest_pli " +
          "ON pli.id = latest_pli.id AND pli.version = latest_pli.max_version " +
          "WHERE pli.job_uid IN (" +
          "  SELECT j.uid FROM jobs j " +
          "  JOIN (SELECT id, MAX(version) as max_version FROM jobs GROUP BY id) latest_j " +
          "  ON j.id = latest_j.id AND j.version = latest_j.max_version " +
          "  WHERE j.contractor_id = :contractorId" +
          ") " +
          "AND pli.timelog_uid IN (" +
          "  SELECT t.uid FROM timelogs t " +
          "  JOIN (SELECT id, MAX(version) as max_version FROM timelogs GROUP BY id) latest_t " +
          "  ON t.id = latest_t.id AND t.version = latest_t.max_version " +
          "  WHERE t.time_start >= :startTime AND t.time_end <= :endTime" +
          ")")
  List<PaymentLineItem> findAllForContractor(
      @Param("contractorId") String contractorId,
      @Param("startTime") Long startTime,
      @Param("endTime") Long endTime);
  // No additional methods needed - all are inherited from the parent interfaces
}