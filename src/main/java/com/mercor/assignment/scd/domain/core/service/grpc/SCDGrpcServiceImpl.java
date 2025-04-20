package com.mercor.assignment.scd.domain.core.service.grpc;

import com.mercor.assignment.scd.domain.common.Entity;
import com.mercor.assignment.scd.domain.core.*;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.exception.EntityNotFoundException;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.service.JobService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Implementation of the SCD Service that provides abstraction over SCD operations
 * Uses entity-specific services based on the requested entity type
 */
@GrpcService
@RequiredArgsConstructor
public class SCDGrpcServiceImpl extends SCDServiceGrpc.SCDServiceImplBase {

  private final JobService jobService;
//  private final TimelogService timelogService;
//  private final PaymentLineItemService paymentLineItemService;

  @Override
  public void getLatestVersion(GetLatestVersionRequest request, StreamObserver<EntityResponse> responseObserver) {
    String entityType = request.getEntityType();
    String id = request.getId();

    // Convert the result to Entity object based on entity type
    Entity entity = findLatestVersionById(entityType, id);

    EntityResponse response = EntityResponse.newBuilder()
        .setEntity(entity)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getVersionHistory(GetVersionHistoryRequest request, StreamObserver<EntityListResponse> responseObserver) {
    String entityType = request.getEntityType();
    String id = request.getId();

    // Find all versions sorted by version number and convert to Entity objects
    List<Entity> versions = findAllVersionsById(entityType, id);

    EntityListResponse response = EntityListResponse.newBuilder()
        .addAllEntities(versions)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void query(QueryRequest request, StreamObserver<EntityListResponse> responseObserver) {
    String entityType = request.getEntityType();
    Map<String, String> conditions = request.getConditionsMap();
    boolean latestVersionOnly = request.getLatestVersionOnly();
    
    // Convert string conditions to appropriate types
    Map<String, Object> typedConditions = convertConditions(entityType, conditions);
    
    // Execute query based on entity type
    List<Entity> results = queryEntities(entityType, typedConditions, latestVersionOnly);

    EntityListResponse response = EntityListResponse.newBuilder()
        .addAllEntities(results)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void update(UpdateRequest request, StreamObserver<EntityResponse> responseObserver) {
    String entityType = request.getEntityType();
    String id = request.getId();
    Map<String, String> fields = request.getFieldsMap();
    
    // Convert string fields to appropriate types
    Map<String, Object> typedFields = convertConditions(entityType, fields);
    
    // Create a new version based on the entity type
    Entity updatedEntity = createNewVersion(entityType, id, typedFields);

    EntityResponse response = EntityResponse.newBuilder()
        .setEntity(updatedEntity)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void batchGet(BatchGetRequest request, StreamObserver<BatchResponse> responseObserver) {
    String entityType = request.getEntityType();
    List<String> ids = request.getIdsList();

    // Fetch latest versions of all requested entities
    Map<String, Entity> entities = new HashMap<>();
    Map<String, String> errors = new HashMap<>();
    
    for (String id : ids) {
      try {
        Entity entity = findLatestVersionById(entityType, id);
        entities.put(id, entity);
      } catch (EntityNotFoundException e) {
        errors.put(id, e.getMessage());
      } catch (Exception e) {
        errors.put(id, "Error processing entity with ID " + id + ": " + e.getMessage());
      }
    }

    BatchResponse response = BatchResponse.newBuilder()
        .addAllEntities(entities.values())
        .putAllErrors(errors)
        .setSuccessCount(entities.size())
        .setFailureCount(errors.size())
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void batchUpdate(BatchUpdateRequest request, StreamObserver<BatchResponse> responseObserver) {
    String entityType = request.getEntityType();
    List<Entity> entities = request.getEntitiesList();
    Map<String, String> commonFields = request.getCommonFieldsMap();
    
    // Convert common fields to appropriate types
    Map<String, Object> typedCommonFields = convertConditions(entityType, commonFields);
    
    // Process batch update
    Map<String, Entity> updatedEntities = new HashMap<>();
    Map<String, String> errors = new HashMap<>();
    
    for (Entity entity : entities) {
      try {
        // Apply common fields to each entity
        Map<String, Object> entityFields = new HashMap<>(typedCommonFields);
        // Add entity-specific fields if needed from the entity object
        
        // Create new version
        Entity updated = createNewVersion(entityType, entity.getId(), entityFields);
        updatedEntities.put(entity.getId(), updated);
      } catch (Exception e) {
        errors.put(entity.getId(), "Error updating entity: " + e.getMessage());
      }
    }

    BatchResponse response = BatchResponse.newBuilder()
        .addAllEntities(updatedEntities.values())
        .putAllErrors(errors)
        .setSuccessCount(updatedEntities.size())
        .setFailureCount(errors.size())
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
  
  // Private helper methods to route requests to appropriate service based on entity type
  
  private Entity findLatestVersionById(String entityType, String id) {
    switch (EntityType.valueOf(entityType.toUpperCase())) {
      case JOBS:
        return convertJobToEntity(jobService.findLatestVersionById(id)
            .orElseThrow(() -> new EntityNotFoundException("Job not found with ID: " + id)));
//      case TIMELOG:
//        return convertTimelogToEntity(timelogService.findLatestVersionById(id)
//            .orElseThrow(() -> new EntityNotFoundException("Timelog not found with ID: " + id)));
//      case PAYMENT_LINE_ITEMS:
//        return convertPaymentLineItemToEntity(paymentLineItemService.findLatestVersionById(id)
//            .orElseThrow(() -> new EntityNotFoundException("Payment line item not found with ID: " + id)));
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }
  
  private List<Entity> findAllVersionsById(String entityType, String id) {
    switch (EntityType.valueOf(entityType.toUpperCase())) {
      case JOBS:
        return jobService.findAllVersionsById(id).stream()
            .map(this::convertJobToEntity)
            .collect(Collectors.toList());
//      case TIMELOG:
//        return timelogService.findAllVersionsById(id).stream()
//            .map(this::convertTimelogToEntity)
//            .collect(Collectors.toList());
//      case PAYMENT_LINE_ITEMS:
//        return paymentLineItemService.findAllVersionsById(id).stream()
//            .map(this::convertPaymentLineItemToEntity)
//            .collect(Collectors.toList());
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }
  
  private List<Entity> queryEntities(String entityType, Map<String, Object> conditions, boolean latestVersionOnly) {
    if (!latestVersionOnly) {
      throw new UnsupportedOperationException("Non-latest version queries are not currently supported");
    }
    
    switch (EntityType.valueOf(entityType.toUpperCase())) {
      case JOBS:
        return jobService.findLatestVersionsByCriteria(conditions).stream()
            .map(this::convertJobToEntity)
            .collect(Collectors.toList());
//      case TIMELOG:
//        return timelogService.findLatestVersionsByCriteria(conditions).stream()
//            .map(this::convertTimelogToEntity)
//            .collect(Collectors.toList());
//      case PAYMENT_LINE_ITEMS:
//        return paymentLineItemService.findLatestVersionsByCriteria(conditions).stream()
//            .map(this::convertPaymentLineItemToEntity)
//            .collect(Collectors.toList());
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }
  
  private Entity createNewVersion(String entityType, String id, Map<String, Object> fields) {
    switch (EntityType.valueOf(entityType.toUpperCase())) {
      case JOBS:
        return convertJobToEntity(jobService.createNewVersion(id, fields));
//      case TIMELOG:
//        return convertTimelogToEntity(timelogService.createNewVersion(id, fields));
//      case PAYMENT_LINE_ITEMS:
//        return convertPaymentLineItemToEntity(paymentLineItemService.createNewVersion(id, fields));
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }
  
  // Conversion methods to transform domain objects to gRPC Entity objects

  private Entity convertJobToEntity(Job job) {
    // Create an Entity.Builder based on your actual protobuf definition
    // This is an example - adjust according to your actual Entity message structure
    Entity.Builder builder = Entity.newBuilder();

    return builder.build();
  }
  
//  private Entity convertTimelogToEntity(Timelog timelog) {
//    // Create an Entity.Builder based on your actual protobuf definition
//    // This is an example - adjust according to your actual Entity message structure
//    Entity.Builder builder = Entity.newBuilder();
//
//    return builder.build();
//  }
//
//  private Entity convertPaymentLineItemToEntity(PaymentLineItem lineItem) {
//    // Create an Entity.Builder based on your actual protobuf definition
//    // This is an example - adjust according to your actual Entity message structure
//    Entity.Builder builder = Entity.newBuilder();
//
//    return builder.build();
//  }
  
  // Helper method to convert string conditions to typed values
  private Map<String, Object> convertConditions(String entityType, Map<String, String> conditions) {
    Map<String, Object> result = new HashMap<>();
    
    switch (EntityType.valueOf(entityType.toUpperCase())) {
      case JOBS:
        // Convert job-specific fields to appropriate types
        conditions.forEach((key, value) -> {
          if (key.equals("rate")) {
            result.put(key, new java.math.BigDecimal(value));
          } else {
            result.put(key, value);
          }
        });
        break;
      case TIMELOG:
        // Convert timelog-specific fields to appropriate types
        conditions.forEach((key, value) -> {
          if (key.equals("duration") || key.equals("timeStart") || key.equals("timeEnd")) {
            result.put(key, Long.parseLong(value));
          } else {
            result.put(key, value);
          }
        });
        break;
      case PAYMENT_LINE_ITEMS:
        // Convert payment line item-specific fields to appropriate types
        conditions.forEach((key, value) -> {
          if (key.equals("amount")) {
            result.put(key, new java.math.BigDecimal(value));
          } else {
            result.put(key, value);
          }
        });
        break;
      default:
        // Just copy as strings for unknown entity types
        result.putAll(conditions);
    }
    
    return result;
  }
}