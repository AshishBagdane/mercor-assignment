package com.mercor.assignment.scd.domain.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base entity class for all SCD entities Implements the SCDEntity interface and provides common properties and behavior
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class AbstractSCDEntity implements SCDEntity {

  /**
   * The ID of the entity (remains the same across versions)
   */
  @Column(name = "id", nullable = false)
  private String id;

  /**
   * The version of the entity Starts at 1 and increments with each update
   */
  @Column(name = "version", nullable = false)
  private Integer version;

  /**
   * The unique identifier for this specific version Changes with each new version
   */
  @Id
  @Column(name = "uid", nullable = false)
  private String uid;

  /**
   * The creation timestamp of this entity version
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private Date createdAt;

  /**
   * The last update timestamp of this entity version
   */
  @Column(name = "updated_at", nullable = false)
  private Date updatedAt;
}