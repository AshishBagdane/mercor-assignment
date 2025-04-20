package com.mercor.assignment.scd.domain.timelog.model;

import com.mercor.assignment.scd.domain.core.model.AbstractSCDEntity;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.job.model.Job;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;
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
@Table(name = "timelogs")
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
      updatable = false
  )
  private Job job;

  @Override
  public Timelog cloneForNewVersion(String uid, int version, Date now) {
    return Timelog.builder()
        .id(this.getId())
        .version(version)
        .uid(uid)
        .createdAt(now)
        .updatedAt(now)
        .duration(this.getDuration())
        .timeStart(this.getTimeStart())
        .timeEnd(this.getTimeEnd())
        .type(this.getType())
        .jobUid(this.getJobUid())
        .build();
  }
}