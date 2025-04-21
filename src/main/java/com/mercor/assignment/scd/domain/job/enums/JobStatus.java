package com.mercor.assignment.scd.domain.job.enums;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum JobStatus {
  ACTIVE("active"),
  EXTENDED("extended"),
  COMPLETED("completed");

  private final String value;
  private Set<JobStatus> allowedTransitions;

  // Static initialization block to set up transitions after enum constants are defined
  static {
    ACTIVE.allowedTransitions = new HashSet<>(Arrays.asList(EXTENDED, COMPLETED));
    EXTENDED.allowedTransitions = new HashSet<>(Arrays.asList(ACTIVE, COMPLETED));
    COMPLETED.allowedTransitions = Collections.emptySet();
  }

  /**
   * Validates if transition from current state to target state is allowed
   * @param targetStatus the status to transition to
   * @return true if transition is allowed, false otherwise
   */
  public boolean canTransitionTo(JobStatus targetStatus) {
    return allowedTransitions.contains(targetStatus);
  }

  /**
   * Performs transition validation and throws exception if not valid
   * @param targetStatus the status to transition to
   * @throws IllegalStateException if transition is not allowed
   */
  public void validateTransition(JobStatus targetStatus) {
    if (!canTransitionTo(targetStatus)) {
      throw new IllegalStateException(
          String.format("Cannot transition from %s to %s", this.value, targetStatus.getValue())
      );
    }
  }

  /**
   * Find a JobStatus by its string value
   * @param value string representation of status
   * @return the matching JobStatus or null if not found
   */
  public static JobStatus fromValue(String value) {
    return Arrays.stream(JobStatus.values())
        .filter(status -> status.getValue().equals(value))
        .findFirst()
        .orElse(null);
  }
}