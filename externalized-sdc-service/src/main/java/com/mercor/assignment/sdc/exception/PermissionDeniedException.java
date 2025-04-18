package com.mercor.assignment.sdc.exception;

/**
 * Exception thrown when a user does not have permission to perform an action.
 */
public class PermissionDeniedException extends SCDException {

    private static final String DEFAULT_ERROR_CODE = "PERMISSION_DENIED";

    public PermissionDeniedException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }

    public PermissionDeniedException(String message, String errorCode) {
        super(message, errorCode);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE);
    }

    public PermissionDeniedException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    /**
     * Create a permission denied exception for a specific resource and action
     * 
     * @param resource The resource the user attempted to access
     * @param action   The action the user attempted to perform
     * @return A new PermissionDeniedException with resource and action details
     */
    public static PermissionDeniedException forResourceAndAction(String resource, String action) {
        String message = String.format("Permission denied to %s resource '%s'", action, resource);
        return new PermissionDeniedException(message);
    }
}