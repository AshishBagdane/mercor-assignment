package com.mercor.assignment.scd.domain.core.model;

import java.util.Date;

/**
 * Base interface for all SCD entities
 */
public interface SCDEntity {

  String getId();

  void setId(String id);

  Integer getVersion();

  void setVersion(Integer version);

  String getUid();

  void setUid(String uid);

  Date getCreatedAt();

  void setCreatedAt(Date createdAt);

  Date getUpdatedAt();

  void setUpdatedAt(Date updatedAt);

  SCDEntity cloneForNewVersion(String uid, int version, Date now);
}