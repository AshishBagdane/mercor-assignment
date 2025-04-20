package com.mercor.assignment.scd.common.errorhandling.exceptions;

/**
 * Base exception class for all application-specific exceptions.
 * Contains error code for more precise error classification.
 */
public class SCDException extends RuntimeException {

  private final String errorCode;

  public SCDException(String message) {
    super(message);
    this.errorCode = "GENERAL_ERROR";
  }

  public SCDException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public SCDException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "GENERAL_ERROR";
  }

  public SCDException(String message, Throwable cause, String errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Get the application-specific error code for this exception.
   * Error codes provide machine-readable categorization of errors.
   * 
   * @return The error code
   */
  public String getErrorCode() {
    return errorCode;
  }
}