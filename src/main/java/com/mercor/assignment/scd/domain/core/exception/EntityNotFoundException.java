package com.mercor.assignment.scd.domain.core.exception;

/**
 * Exception thrown when an entity cannot be found
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with the specified message
     *
     * @param message the detail message
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}