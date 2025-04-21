package com.mercor.assignment.scd.domain.paymentlineitem.enums;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum PaymentLineItemStatus {
  NOT_PAID("not-paid"),
  PROCESSING("processing"),
  PAID("paid");

  private final String value;
  private Set<PaymentLineItemStatus> allowedTransitions;

  // Static initialization block to set up transitions after enum constants are defined
  static {
    NOT_PAID.allowedTransitions = new HashSet<>(Collections.singletonList(PROCESSING));
    PROCESSING.allowedTransitions = new HashSet<>(Arrays.asList(NOT_PAID, PAID));
    PAID.allowedTransitions = Collections.emptySet();
  }

  /**
   * Validates if transition from current state to target state is allowed
   * @param targetStatus the status to transition to
   * @return true if transition is allowed, false otherwise
   */
  public boolean canTransitionTo(PaymentLineItemStatus targetStatus) {
    return allowedTransitions.contains(targetStatus);
  }

  /**
   * Performs transition validation and throws exception if not valid
   * @param targetStatus the status to transition to
   * @throws IllegalStateException if transition is not allowed
   */
  public void validateTransition(PaymentLineItemStatus targetStatus) {
    if (!canTransitionTo(targetStatus)) {
      throw new IllegalStateException(
          String.format("Cannot transition from %s to %s", this.value, targetStatus.getValue())
      );
    }
  }

  /**
   * Find a PaymentLineItemStatus by its string value
   * @param value string representation of status
   * @return the matching PaymentLineItemStatus or null if not found
   */
  public static PaymentLineItemStatus fromValue(String value) {
    return Arrays.stream(PaymentLineItemStatus.values())
        .filter(status -> status.getValue().equals(value))
        .findFirst()
        .orElse(null);
  }
}