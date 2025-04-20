package com.mercor.assignment.scd.domain.paymentlineitem.model;

import com.mercor.assignment.scd.domain.core.model.AbstractSCDEntity;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity class for PaymentLineItem
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "payment_line_items")
public class PaymentLineItem extends AbstractSCDEntity {

  /**
   * The UID of the job associated with the payment line item
   */
  @Column(name = "job_uid", nullable = false)
  private String jobUid;

  /**
   * The UID of the timelog associated with the payment line item
   */
  @Column(name = "timelog_uid", nullable = false)
  private String timelogUid;

  /**
   * The amount of the payment
   */
  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  /**
   * The status of the payment (e.g., "paid", "not-paid")
   */
  @Column(name = "status", nullable = false)
  private String status;

  /**
   * The job entity associated with the payment line item This is the JPA relationship to the Job entity
   */
  @ManyToOne
  @JoinColumn(
      name = "job_uid",
      referencedColumnName = "uid",
      insertable = false,
      updatable = false
  )
  private Job job;

  /**
   * The timelog entity associated with the payment line item This is the JPA relationship to the Timelog entity
   */
  @ManyToOne
  @JoinColumn(
      name = "timelog_uid",
      referencedColumnName = "uid",
      insertable = false,
      updatable = false
  )
  private Timelog timelog;

  @Override
  public PaymentLineItem cloneForNewVersion(String uid, int version, Date now) {
    return PaymentLineItem.builder()
        .id(this.getId())
        .version(version)
        .uid(uid)
        .createdAt(now)
        .updatedAt(now)
        .timelogUid(this.getTimelogUid())
        .jobUid(this.getJobUid())
        .amount(this.getAmount())
        .status(this.getStatus())
        .build();
  }
}