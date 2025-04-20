package com.mercor.assignment.scd.domain.core.enums;

import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EntityType {
  JOBS("jobs", "job", ServiceName.JOB_SERVICE),
  TIMELOG("timelog", "tl", ServiceName.TIMELOG_SERVICE),
  PAYMENT_LINE_ITEMS("payment_line_items", "pli", ServiceName.PAYMENT_LINE_ITEMS_SERVICE),;

  private final String value;
  private final String prefix;
  private final String serviceName;

  public static EntityType fromValue(String value) {
    for (EntityType type : EntityType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown entity type: " + value);
  }
}
