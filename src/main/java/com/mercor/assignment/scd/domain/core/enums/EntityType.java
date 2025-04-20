package com.mercor.assignment.scd.domain.core.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EntityType {
  JOBS("jobs", "job"),
  TIMELOG("timelog", "tl"),
  PAYMENT_LINE_ITEMS("payment_line_items", "pli"),;

  private final String value;
  private final String prefix;
}
