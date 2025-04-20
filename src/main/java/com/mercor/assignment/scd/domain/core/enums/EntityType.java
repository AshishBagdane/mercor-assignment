package com.mercor.assignment.scd.domain.core.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EntityType {
  JOBS("jobs"),
  TIMELOG("timelog"),
  PAYMENT_LINE_ITEMS("payment_line_items"),;

  private final String value;
}
