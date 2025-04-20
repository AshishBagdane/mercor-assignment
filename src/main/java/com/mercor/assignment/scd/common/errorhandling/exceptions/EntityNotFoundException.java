package com.mercor.assignment.scd.common.errorhandling.exceptions;

/**
 * Exception thrown when an entity is not found in the system.
 */
public class EntityNotFoundException extends SCDException {

  private static final String DEFAULT_ERROR_CODE = "ENTITY_NOT_FOUND";

  public EntityNotFoundException(String message) {
    super(message, DEFAULT_ERROR_CODE);
  }

  public EntityNotFoundException(String message, String errorCode) {
    super(message, errorCode);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause, DEFAULT_ERROR_CODE);
  }

  public EntityNotFoundException(String message, Throwable cause, String errorCode) {
    super(message, cause, errorCode);
  }

  /**
   * Create an EntityNotFoundException specific to the entity type
   * 
   * @param entityType The type of entity that was not found
   * @param id         The ID of the entity that was not found
   * @return A new EntityNotFoundException with a type-specific message and error
   *         code
   */
  public static EntityNotFoundException forEntity(String entityType, String id) {
    String message = String.format("%s with ID '%s' not found", entityType, id);
    String errorCode = entityType.toUpperCase() + "_NOT_FOUND";
    return new EntityNotFoundException(message, errorCode);
  }
}