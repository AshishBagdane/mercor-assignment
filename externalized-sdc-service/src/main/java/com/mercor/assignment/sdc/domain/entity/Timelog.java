package com.mercor.assignment.sdc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity class for Timelog
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(
    name = "timelogs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_timelogs_id_version", columnNames = {"id", "version"})
    },
    indexes = {
        @Index(name = "idx_timelogs_id", columnList = "id"),
        @Index(name = "idx_timelogs_job_uid", columnList = "job_uid"),
        @Index(name = "idx_timelogs_id_version", columnList = "id, version DESC"),
        @Index(name = "idx_timelogs_job_uid_time_start_time_end", columnList = "job_uid, time_start, time_end")
    }
)
public class Timelog extends AbstractSCDEntity {

  /**
   * The duration of the time entry in milliseconds
   */
  @Column(name = "duration", nullable = false)
  private Long duration;

  /**
   * The start timestamp of the time entry in milliseconds
   */
  @Column(name = "time_start", nullable = false)
  private Long timeStart;

  /**
   * The end timestamp of the time entry in milliseconds
   */
  @Column(name = "time_end", nullable = false)
  private Long timeEnd;

  /**
   * The type of the time entry (e.g., "captured", "adjusted")
   */
  @Column(name = "type", nullable = false)
  private String type;

  /**
   * The UID of the job associated with the time entry
   */
  @Column(name = "job_uid", nullable = false)
  private String jobUid;

  /**
   * The job entity associated with the time entry This is the JPA relationship to the Job entity
   */
  @ManyToOne
  @JoinColumn(
      name = "job_uid",
      referencedColumnName = "uid",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_timelogs_jobs")
  )
  private Job job;
}