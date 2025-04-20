package com.mercor.assignment.scd.domain.job.model;

import com.mercor.assignment.scd.domain.core.model.AbstractSCDEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
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
@Table(name = "jobs")
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

  @Override
  public Job cloneForNewVersion(String uid, int version, Date now) {
    return Job.builder()
        .id(this.getId())
        .version(version)
        .uid(uid)
        .createdAt(now)
        .updatedAt(now)
        .status(this.getStatus())
        .rate(this.getRate())
        .title(this.getTitle())
        .companyId(this.getCompanyId())
        .contractorId(this.getContractorId())
        .build();
  }
}