package com.mercor.assignment.sdc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity class for Job
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(
    name = "jobs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_jobs_id_version", columnNames = {"id", "version"})
    },
    indexes = {
        @Index(name = "idx_jobs_id", columnList = "id"),
        @Index(name = "idx_jobs_company_id", columnList = "company_id"),
        @Index(name = "idx_jobs_contractor_id", columnList = "contractor_id"),
        @Index(name = "idx_jobs_id_version", columnList = "id, version DESC"),
        @Index(name = "idx_jobs_company_id_id_version", columnList = "company_id, id, version DESC"),
        @Index(name = "idx_jobs_contractor_id_id_version", columnList = "contractor_id, id, version DESC")
    }
)
public class Job extends AbstractSCDEntity {

  /**
   * The status of the job (e.g., "active", "extended")
   */
  @Column(name = "status", nullable = false)
  private String status;

  /**
   * The hourly rate for the job
   */
  @Column(name = "rate", nullable = false, precision = 10, scale = 2)
  private BigDecimal rate;

  /**
   * The title of the job
   */
  @Column(name = "title", nullable = false)
  private String title;

  /**
   * The ID of the company associated with the job
   */
  @Column(name = "company_id", nullable = false)
  private String companyId;

  /**
   * The ID of the contractor associated with the job
   */
  @Column(name = "contractor_id", nullable = false)
  private String contractorId;
}