package com.mercor.assignment.scd.domain.timelog.enums;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum TimelogType {
  CAPTURED("captured"),
  ADJUSTED("adjusted");

  private final String value;
  private Set<TimelogType> allowedTransitions;

  // Static initialization block to set up transitions after enum constants are defined
  static {
    CAPTURED.allowedTransitions = new HashSet<>(Collections.singletonList(ADJUSTED));
    ADJUSTED.allowedTransitions = Collections.emptySet(); // Terminal state, no further transitions
  }

  /**
   * Validates if transition from current state to target state is allowed
   * @param targetType the type to transition to
   * @return true if transition is allowed, false otherwise
   */
  public boolean canTransitionTo(TimelogType targetType) {
    return allowedTransitions.contains(targetType);
  }

  /**
   * Performs transition validation and throws exception if not valid
   * @param targetType the type to transition to
   * @throws IllegalStateException if transition is not allowed
   */
  public void validateTransition(TimelogType targetType) {
    if (!canTransitionTo(targetType)) {
      throw new IllegalStateException(
          String.format("Cannot transition from %s to %s", this.value, targetType.getValue())
      );
    }
  }

  /**
   * Find a TimelogType by its string value
   * @param value string representation of type
   * @return the matching TimelogType or null if not found
   */
  public static TimelogType fromValue(String value) {
    return Arrays.stream(TimelogType.values())
        .filter(type -> type.getValue().equals(value))
        .findFirst()
        .orElse(null);
  }
}