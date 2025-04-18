package com.mercor.assignment.sdc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
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
@Table(
    name = "payment_line_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_line_items_id_version", columnNames = {"id", "version"})
    },
    indexes = {
        @Index(name = "idx_payment_line_items_id", columnList = "id"),
        @Index(name = "idx_payment_line_items_job_uid", columnList = "job_uid"),
        @Index(name = "idx_payment_line_items_timelog_uid", columnList = "timelog_uid"),
        @Index(name = "idx_payment_line_items_id_version", columnList = "id, version DESC"),
        @Index(name = "idx_payment_line_items_job_uid_status", columnList = "job_uid, status")
    }
)
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
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_payment_line_items_jobs")
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
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_payment_line_items_timelogs")
  )
  private Timelog timelog;
}