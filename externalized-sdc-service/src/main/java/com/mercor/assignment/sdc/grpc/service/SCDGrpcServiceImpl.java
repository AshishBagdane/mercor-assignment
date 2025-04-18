package com.mercor.assignment.sdc.grpc.service;

import com.google.protobuf.ByteString;
import com.mercor.assignment.sdc.domain.dto.JobDTO;
import com.mercor.assignment.sdc.domain.dto.PaymentLineItemDTO;
import com.mercor.assignment.sdc.domain.dto.SCDBatchRequest;
import com.mercor.assignment.sdc.domain.dto.SCDBatchResponse;
import com.mercor.assignment.sdc.domain.dto.SCDEntityDTO;
import com.mercor.assignment.sdc.domain.dto.SCDQueryRequest;
import com.mercor.assignment.sdc.domain.dto.SCDUpdateRequest;
import com.mercor.assignment.sdc.domain.dto.TimelogDTO;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.service.JobService;
import com.mercor.assignment.sdc.service.PaymentLineItemService;
import com.mercor.assignment.sdc.service.SCDService;
import com.mercor.assignment.sdc.service.TimelogService;
import com.mercor.scd.grpc.BatchGetRequest;
import com.mercor.scd.grpc.BatchResponse;
import com.mercor.scd.grpc.BatchUpdateRequest;
import com.mercor.scd.grpc.Entity;
import com.mercor.scd.grpc.EntityListResponse;
import com.mercor.scd.grpc.EntityResponse;
import com.mercor.scd.grpc.GetLatestVersionRequest;
import com.mercor.scd.grpc.GetVersionHistoryRequest;
import com.mercor.scd.grpc.QueryRequest;
import com.mercor.scd.grpc.SCDServiceGrpc;
import com.mercor.scd.grpc.UpdateRequest;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Implementation of the gRPC SCDService
 */
@Slf4j
@Service
public class SCDGrpcServiceImpl extends SCDServiceGrpc.SCDServiceImplBase {

  private final Map<String, SCDService<?>> serviceMap;

  @Autowired
  public SCDGrpcServiceImpl(
      @Qualifier("jobServiceImpl") JobService jobService,
      @Qualifier("timelogServiceImpl") TimelogService timelogService,
      @Qualifier("paymentLineItemServiceImpl") PaymentLineItemService paymentLineItemService) {

    this.serviceMap = new HashMap<>();
    this.serviceMap.put("job", jobService);
    this.serviceMap.put("timelog", timelogService);
    this.serviceMap.put("payment_line_item", paymentLineItemService);
  }

  @Override
  public void getLatestVersion(GetLatestVersionRequest request, StreamObserver<EntityResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService<?> service = getServiceForType(entityType);

      SCDEntityDTO dto = service.getLatestVersion(request.getId());
      Entity entity = convertDtoToEntity(dto, entityType);

      EntityResponse response = EntityResponse.newBuilder()
          .setEntity(entity)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid request: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in getLatestVersion call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void getVersionHistory(GetVersionHistoryRequest request, StreamObserver<EntityListResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService<?> service = getServiceForType(entityType);

      List<? extends SCDEntityDTO> dtoList = service.getVersionHistory(request.getId());
      List<Entity> entities = dtoList.stream()
          .map(dto -> convertDtoToEntity(dto, entityType))
          .toList();

      EntityListResponse response = EntityListResponse.newBuilder()
          .addAllEntities(entities)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found in getVersionHistory: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument in getVersionHistory: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid request: " + e.getMessage(),
          e));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument in getVersionHistory: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid request parameters: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in getVersionHistory call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during version history retrieval: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void query(QueryRequest request, StreamObserver<EntityListResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService<?> service = getServiceForType(entityType);

      // Convert the conditions map
      Map<String, Object> conditions = new HashMap<>();
      for (Map.Entry<String, String> entry : request.getConditionsMap().entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        try {
          Object convertedValue = convertStringToTypedValue(value);
          conditions.put(key, convertedValue);
        } catch (Exception e) {
          log.warn("Error converting value for condition {}: {}", key, e.getMessage());
          throw new SCDException("Invalid value for condition '" + key + "': " + e.getMessage(),
              "INVALID_CONDITION_VALUE");
        }
      }

      // Create and populate the query request
      SCDQueryRequest queryRequest = SCDQueryRequest.builder()
          .conditions(conditions)
          .latestVersionOnly(request.getLatestVersionOnly())
          .limit(request.getLimit() > 0 ? request.getLimit() : null)
          .offset(request.getOffset() > 0 ? request.getOffset() : null)
          .sortBy(request.getSortBy().isEmpty() ? null : request.getSortBy())
          .sortDirection(request.getSortDirection().isEmpty() ? null : request.getSortDirection())
          .build();

      // Execute the query with proper error handling
      List<?> dtoList;
      try {
        dtoList = service.query(queryRequest);
      } catch (SCDException e) {
        log.warn("Error executing query: {}", e.getMessage());
        throw e;
      } catch (Exception e) {
        log.error("Unexpected error during query: {}", e.getMessage());
        throw new SCDException("Error executing query: " + e.getMessage(), e, "QUERY_ERROR");
      }

      // Convert results to gRPC entities
      List<Entity> entities = dtoList.stream()
          .map(dto -> convertDtoToEntity((SCDEntityDTO) dto, entityType))
          .collect(Collectors.toList());

      EntityListResponse response = EntityListResponse.newBuilder()
          .addAllEntities(entities)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found in query: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument in query: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid query parameters: " + e.getMessage(),
          e));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument in query: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid query parameters: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in query call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during query operation: " + e.getMessage(),
          e));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(UpdateRequest request, StreamObserver<EntityResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService service = getServiceForType(entityType);

      // Convert the fields map
      Map<String, Object> fields = new HashMap<>();
      for (Map.Entry<String, String> entry : request.getFieldsMap().entrySet()) {
        fields.put(entry.getKey(), convertStringToTypedValue(entry.getValue()));
      }

      // Create and populate the update request
      SCDUpdateRequest updateRequest = new SCDUpdateRequest<>();
      updateRequest.setFields(fields);

      // If entity is provided, convert it
      if (request.hasEntity()) {
        updateRequest.setEntity(convertEntityToDto(request.getEntity(), entityType));
      }

      SCDEntityDTO updatedDto = (SCDEntityDTO) service.update(request.getId(), updateRequest);
      Entity entity = convertDtoToEntity(updatedDto, entityType);

      EntityResponse response = EntityResponse.newBuilder()
          .setEntity(entity)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found in update: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found for update: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument in update: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid update parameters: " + e.getMessage(),
          e));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument in update: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid update parameters: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in update call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during update: " + e.getMessage(),
          e));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void batchGet(BatchGetRequest request, StreamObserver<BatchResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService service = getServiceForType(entityType);

      SCDBatchRequest batchRequest = new SCDBatchRequest();
      batchRequest.setIds(request.getIdsList());

      SCDBatchResponse batchResponse = service.batchGet(batchRequest);

      // Create a list of converted entities
      List<Entity> entityList = new ArrayList<>();
      if (batchResponse.getEntities() != null) {
        for (Object dto : batchResponse.getEntities()) {
          if (dto instanceof SCDEntityDTO) {
            entityList.add(convertDtoToEntity((SCDEntityDTO) dto, entityType));
          }
        }
      }

      BatchResponse response = BatchResponse.newBuilder()
          .addAllEntities(entityList)
          .putAllErrors(batchResponse.getErrors())
          .setSuccessCount(batchResponse.getSuccessCount())
          .setFailureCount(batchResponse.getFailureCount())
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found in batchGet: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument in batchGet: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid batch request: " + e.getMessage(),
          e));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument in batchGet: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid batch request: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in batchGet call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during batch get: " + e.getMessage(),
          e));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void batchUpdate(BatchUpdateRequest request, StreamObserver<BatchResponse> responseObserver) {
    try {
      String entityType = request.getEntityType();
      SCDService service = getServiceForType(entityType);

      // Validate entities in the request
      if (request.getEntitiesList() == null || request.getEntitiesList().isEmpty()) {
        throw new SCDException("Batch update request must contain at least one entity", "INVALID_REQUEST");
      }

      // Convert the common fields map
      Map<String, Object> commonFields = new HashMap<>();
      for (Map.Entry<String, String> entry : request.getCommonFieldsMap().entrySet()) {
        try {
          commonFields.put(entry.getKey(), convertStringToTypedValue(entry.getValue()));
        } catch (Exception e) {
          throw new SCDException("Invalid field value for field '" + entry.getKey() + "': " + e.getMessage(),
              "INVALID_FIELD_VALUE");
        }
      }

      SCDBatchRequest batchUpdateRequest = new SCDBatchRequest();
      List<SCDEntityDTO> convertedEntities = new ArrayList<>();

      // Convert entities with error handling for each one
      for (Entity entity : request.getEntitiesList()) {
        try {
          SCDEntityDTO dto = convertEntityToDto(entity, entityType);
          if (dto.getId() == null || dto.getId().isEmpty()) {
            throw new SCDException("Entity ID cannot be null or empty", "INVALID_ENTITY_ID");
          }
          convertedEntities.add(dto);
        } catch (Exception e) {
          throw new SCDException("Error converting entity: " + e.getMessage(), "ENTITY_CONVERSION_ERROR");
        }
      }

      batchUpdateRequest.setEntities(convertedEntities);
      batchUpdateRequest.setCommonFields(commonFields);

      SCDBatchResponse batchResponse = service.batchUpdate(batchUpdateRequest);

      // Create a list of converted entities
      List<Entity> entityList = new ArrayList<>();
      if (batchResponse.getEntities() != null) {
        for (Object dto : batchResponse.getEntities()) {
          if (dto instanceof SCDEntityDTO) {
            entityList.add(convertDtoToEntity((SCDEntityDTO) dto, entityType));
          }
        }
      }

      BatchResponse response = BatchResponse.newBuilder()
          .addAllEntities(entityList)
          .putAllErrors(batchResponse.getErrors())
          .setSuccessCount(batchResponse.getSuccessCount())
          .setFailureCount(batchResponse.getFailureCount())
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      log.warn("Entity not found in batchUpdate: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Entity not found for update: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      log.warn("Invalid argument in batchUpdate: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid batch update parameters: " + e.getMessage(),
          e));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid argument in batchUpdate: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          "Invalid batch update parameters: " + e.getMessage(),
          e));
    } catch (Exception e) {
      log.error("Error in batchUpdate call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during batch update: " + e.getMessage(),
          e));
    }
  }

  /**
   * Get the service instance for a specific entity type
   *
   * @param entityType The entity type
   * @return The service instance
   */
  private SCDService<?> getServiceForType(String entityType) {
    if (entityType == null || entityType.isEmpty()) {
      throw new IllegalArgumentException("Entity type cannot be null or empty");
    }

    // Normalize entity type to lowercase and remove underscores
    String normalizedType = entityType.toLowerCase().replace("_", "");

    // First try exact match
    SCDService<?> service = serviceMap.get(normalizedType);
    if (service != null) {
      return service;
    }

    // Try alternate mappings for common variations
    if (normalizedType.equals("paymentlineitem")) {
      service = serviceMap.get("payment_line_item");
      if (service != null) {
        return service;
      }
    }

    // Check if any service name contains the normalized type
    for (Map.Entry<String, SCDService<?>> entry : serviceMap.entrySet()) {
      if (entry.getKey().contains(normalizedType) || normalizedType.contains(entry.getKey())) {
        log.warn("Using approximate entity type match: {} -> {}", entityType, entry.getKey());
        return entry.getValue();
      }
    }

    // No match found
    throw new IllegalArgumentException("Unknown entity type: " + entityType);
  }

  /**
   * Convert a DTO to a gRPC Entity
   *
   * @param dto        The DTO to convert
   * @param entityType The entity type
   * @return The gRPC Entity
   */
  private Entity convertDtoToEntity(SCDEntityDTO dto, String entityType) {
    Entity.Builder builder = Entity.newBuilder()
        .setType(entityType)
        .setId(dto.getId())
        .setVersion(dto.getVersion())
        .setUid(dto.getUid())
        .setCreatedAt(dto.getCreatedAt().getTime())
        .setUpdatedAt(dto.getUpdatedAt().getTime());

    // Serialize entity-specific data
    ByteString data = serializeEntityData(dto, entityType);
    if (data != null) {
      builder.setData(data);
    }

    return builder.build();
  }

  /**
   * Convert a gRPC Entity to a DTO
   *
   * @param entity     The gRPC Entity to convert
   * @param entityType The entity type
   * @return The DTO
   */
  private SCDEntityDTO convertEntityToDto(Entity entity, String entityType) {
    SCDEntityDTO dto;

    String normalizedType = entityType.toLowerCase().replace("_", "");

    try {
      if (normalizedType.contains("job")) {
        dto = new JobDTO();
      } else if (normalizedType.contains("timelog")) {
        dto = new TimelogDTO();
      } else if (normalizedType.contains("payment")) {
        dto = new PaymentLineItemDTO();
      } else {
        throw new IllegalArgumentException("Unknown entity type: " + entityType);
      }

      dto.setId(entity.getId());
      dto.setVersion(entity.getVersion());
      dto.setUid(entity.getUid());
      dto.setCreatedAt(new Date(entity.getCreatedAt()));
      dto.setUpdatedAt(new Date(entity.getUpdatedAt()));

      // Deserialize entity-specific data
      if (entity.getData() != null && !entity.getData().isEmpty()) {
        deserializeEntityData(dto, entity.getData(), entityType);
      }

      return dto;
    } catch (Exception e) {
      log.error("Error converting entity to DTO: {}", e.getMessage(), e);
      throw new IllegalArgumentException("Error converting entity type: " + entityType + ": " + e.getMessage());
    }
  }

  /**
   * Serialize entity-specific data to ByteString
   *
   * @param dto        The DTO to serialize
   * @param entityType The entity type
   * @return The serialized data
   */
  private ByteString serializeEntityData(SCDEntityDTO dto, String entityType) {
    // This is a simplified implementation
    // In a real-world scenario, you would use a proper serialization mechanism
    // such as Protocol Buffers, JSON, or a binary format

    // For the sake of this example, we'll return null
    return null;
  }

  /**
   * Deserialize entity-specific data from ByteString
   *
   * @param dto        The DTO to populate
   * @param data       The serialized data
   * @param entityType The entity type
   */
  private void deserializeEntityData(SCDEntityDTO dto, ByteString data, String entityType) {
    // This is a simplified implementation
    // In a real-world scenario, you would use a proper deserialization mechanism
    // such as Protocol Buffers, JSON, or a binary format
  }

  /**
   * Convert a string value to a typed value
   *
   * @param value The string value
   * @return The typed value
   */
  private Object convertStringToTypedValue(String value) {
    // Try to infer the type from the value
    if (value == null || value.isEmpty()) {
      return null;
    }

    // Handle special values
    if (value.equalsIgnoreCase("null")) {
      return null;
    }

    if (value.equalsIgnoreCase("true")) {
      return Boolean.TRUE;
    }

    if (value.equalsIgnoreCase("false")) {
      return Boolean.FALSE;
    }

    // Check if it's a timestamp in milliseconds
    if (value.matches("^\\d{13}$")) {
      try {
        long timestamp = Long.parseLong(value);
        // Simple validation - valid timestamps should be between 2000 and 2100
        if (timestamp > 946684800000L && timestamp < 4102444800000L) {
          return timestamp; // Return as Long
        }
      } catch (NumberFormatException ignored) {
        // Continue with other type checks
      }
    }

    // Try numeric types in order of precision
    try {
      if (value.contains(".")) {
        // Try BigDecimal first for decimal values
        return new BigDecimal(value);
      } else {
        // For integers, try Integer first then Long
        int intValue = Integer.parseInt(value);
        return intValue;
      }
    } catch (NumberFormatException e1) {
      try {
        long longValue = Long.parseLong(value);
        return longValue;
      } catch (NumberFormatException e2) {
        try {
          double doubleValue = Double.parseDouble(value);
          return doubleValue;
        } catch (NumberFormatException e3) {
          try {
            return new BigDecimal(value);
          } catch (NumberFormatException e4) {
            // Not a numeric value
          }
        }
      }
    }

    // Handle date format strings (simplified)
    if (value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
      try {
        // Simple ISO date parsing
        return new SimpleDateFormat("yyyy-MM-dd").parse(value).getTime();
      } catch (Exception e) {
        // Not a valid date format, continue
      }
    }

    // Default to string
    return value;
  }

  /**
   * Create a proper gRPC error response with detailed metadata
   *
   * @param status  The gRPC status code
   * @param message The error message
   * @param error   The original exception
   * @return StatusRuntimeException with detailed metadata
   */
  private StatusRuntimeException createDetailedError(Status status, String message, Throwable error) {
    Metadata metadata = new Metadata();
    Metadata.Key<String> errorDetailsKey = Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(errorDetailsKey, error.getMessage());

    if (error.getCause() != null) {
      metadata.put(
          Metadata.Key.of("error-cause", Metadata.ASCII_STRING_MARSHALLER),
          error.getCause().getMessage());
    }

    // Add debugging info in development environments
    StackTraceElement[] stackTrace = error.getStackTrace();
    if (stackTrace.length > 0) {
      String location = stackTrace[0].toString();
      metadata.put(
          Metadata.Key.of("error-location", Metadata.ASCII_STRING_MARSHALLER),
          location);
    }

    return status
        .withDescription(message)
        .withCause(error)
        .asRuntimeException(metadata);
  }
}