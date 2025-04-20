package com.mercor.assignment.scd.common.errorhandling.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when input validation fails.
 * Can contain multiple validation errors with field references.
 */
public class ValidationException extends SCDException {

    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private final List<ValidationError> validationErrors = new ArrayList<>();

    public ValidationException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }

    public ValidationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE);
    }

    public ValidationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    /**
     * Create a validation exception for a specific field error
     * 
     * @param field   The field that failed validation
     * @param message The validation error message
     * @return A new ValidationException with field details
     */
    public static ValidationException forField(String field, String message) {
        ValidationException ex = new ValidationException("Validation error: " + message);
        ex.addValidationError(field, message);
        return ex;
    }

    /**
     * Add a validation error for a specific field
     * 
     * @param field   The field that failed validation
     * @param message The validation error message
     * @return This exception instance for method chaining
     */
    public ValidationException addValidationError(String field, String message) {
        this.validationErrors.add(new ValidationError(field, message));
        return this;
    }

    /**
     * Get all validation errors
     * 
     * @return An unmodifiable list of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    /**
     * Check if this exception contains field-specific validation errors
     * 
     * @return true if there are field validation errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * Internal class representing a single field validation error
     */
    public static class ValidationError {
        private final String field;
        private final String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return field + ": " + message;
        }
    }
}