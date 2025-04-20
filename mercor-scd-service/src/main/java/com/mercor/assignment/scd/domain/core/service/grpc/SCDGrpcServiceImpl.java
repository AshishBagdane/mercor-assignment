package com.mercor.assignment.scd.domain.core.service.grpc;

import com.mercor.assignment.scd.common.errorhandling.exceptions.EntityNotFoundException;
import com.mercor.assignment.scd.domain.common.Entity;
import com.mercor.assignment.scd.domain.core.*;
import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.mapper.EntityMapper;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.core.service.SCDService;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.service.JobService;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitem.service.regular.PaymentLineItemService;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import com.mercor.assignment.scd.domain.timelog.service.regular.TimelogService;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Implementation of the SCD Service that provides abstraction over SCD operations
 * Uses entity-specific services based on the requested entity type
 */
@Slf4j
@GrpcService
public class SCDGrpcServiceImpl extends SCDServiceGrpc.SCDServiceImplBase {

  private final Map<String, SCDService<?>> serviceMap;

  public SCDGrpcServiceImpl(JobService jobService, TimelogService timelogService, PaymentLineItemService paymentLineItemService){
    this.serviceMap = new HashMap<>();
    serviceMap.put(ServiceName.JOB_SERVICE, jobService);
    serviceMap.put(ServiceName.TIMELOG_SERVICE, timelogService);
    serviceMap.put(ServiceName.PAYMENT_LINE_ITEMS_SERVICE, paymentLineItemService);
  }

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

  private SCDService<?> getServiceForType(String serviceName) {
    if (serviceName == null) {
      throw new IllegalArgumentException("Unknown service type: " + serviceName);
    }
    return serviceMap.get(serviceName);
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
    final EntityType type = EntityType.fromValue(entityType);
    SCDService<?> service = getServiceForType(type.getServiceName());

    switch (type) {
      case JOBS:
        Job job = (Job) service.findLatestVersionById(id).orElseThrow(() -> new EntityNotFoundException("Job with ID " + id + " not found!"));
        return EntityMapper.INSTANCE.mapJobToEntityProto(job);
      case TIMELOG:
        Timelog timelog = (Timelog) service.findLatestVersionById(id).orElseThrow(() -> new EntityNotFoundException("Timelog with ID " + id + " not found!"));
        return EntityMapper.INSTANCE.mapTimelogToEntityProto(timelog);
      case PAYMENT_LINE_ITEMS:
        PaymentLineItem paymentLineItem = (PaymentLineItem) service.findLatestVersionById(id).orElseThrow(() -> new EntityNotFoundException("Paymen tline item with ID " + id + " not found!"));
        return EntityMapper.INSTANCE.mapPaymentLineItemToEntityProto(paymentLineItem);
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }

  private List<Entity> findAllVersionsById(String entityType, String id) {
    final EntityType type = EntityType.fromValue(entityType);
    SCDService<?> service = getServiceForType(type.getServiceName());

    switch (type) {
      case JOBS:
        List<Job> jobs = (List<Job>) service.findAllVersionsById(id);
        return jobs.stream().map(EntityMapper.INSTANCE::mapJobToEntityProto).toList();
      case TIMELOG:
        List<Timelog> timelogs = (List<Timelog>) service.findAllVersionsById(id);
        return timelogs.stream().map(EntityMapper.INSTANCE::mapTimelogToEntityProto).toList();
      case PAYMENT_LINE_ITEMS:
        List<PaymentLineItem> paymentLineItems = (List<PaymentLineItem>) service.findAllVersionsById(id);
        return paymentLineItems.stream().map(EntityMapper.INSTANCE::mapPaymentLineItemToEntityProto).toList();
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }

  private List<Entity> queryEntities(String entityType, Map<String, Object> conditions, boolean latestVersionOnly) {
    if (!latestVersionOnly) {
      throw new UnsupportedOperationException("Non-latest version queries are not currently supported");
    }

    final EntityType type = EntityType.fromValue(entityType);
    SCDService<? extends SCDEntity> service = getServiceForType(type.getServiceName());

    switch (type) {
      case JOBS:
        List<Job> jobs = (List<Job>) service.findLatestVersionsByCriteria(conditions);
        return jobs.stream().map(EntityMapper.INSTANCE::mapJobToEntityProto).toList();
      case TIMELOG:
        List<Timelog> timelog = (List<Timelog>) service.findLatestVersionsByCriteria(conditions);
        return timelog.stream().map(EntityMapper.INSTANCE::mapTimelogToEntityProto).toList();
      case PAYMENT_LINE_ITEMS:
        List<PaymentLineItem> paymentLineItems = (List<PaymentLineItem>) service.findLatestVersionsByCriteria(conditions);
        return paymentLineItems.stream().map(EntityMapper.INSTANCE::mapPaymentLineItemToEntityProto).toList();
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }

  private Entity createNewVersion(String entityType, String id, Map<String, Object> fields) {
    final EntityType type = EntityType.fromValue(entityType);
    SCDService<? extends SCDEntity> service = getServiceForType(type.getServiceName());

    switch (type) {
      case JOBS:
        final Job job = (Job) service.createNewVersion(id, fields);
        return EntityMapper.INSTANCE.mapJobToEntityProto(job);
      case TIMELOG:
        final Timelog timelog = (Timelog) service.createNewVersion(id, fields);
        return EntityMapper.INSTANCE.mapTimelogToEntityProto(timelog);
      case PAYMENT_LINE_ITEMS:
        final PaymentLineItem paymentLineItem = (PaymentLineItem) service.createNewVersion(id, fields);
        return EntityMapper.INSTANCE.mapPaymentLineItemToEntityProto(paymentLineItem);
      default:
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }
  }

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