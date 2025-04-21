package com.mercor.assignment.scd.domain.job.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum JobStatus {
  ACTIVE("active"),
  EXTENDED("extended"),
  COMPLETED("completed");

  private final String value;
}
