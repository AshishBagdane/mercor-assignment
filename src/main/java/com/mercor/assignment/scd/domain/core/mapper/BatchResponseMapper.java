package com.mercor.assignment.scd.domain.core.mapper;

import com.mercor.assignment.scd.domain.common.Entity;
import com.mercor.assignment.scd.domain.core.BatchResponse;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for converting SCD entities to BatchResponse protocol buffer messages
 */
@Mapper(uses = {EntityMapper.class})
public interface BatchResponseMapper {

    BatchResponseMapper INSTANCE = Mappers.getMapper(BatchResponseMapper.class);
    
    /**
     * Maps a list of SCDEntity objects and error map to a BatchResponse
     */
    default BatchResponse mapToBatchResponse(List<? extends SCDEntity> entities, Map<String, String> errors) {
        EntityMapper entityMapper = EntityMapper.INSTANCE;
        
        List<Entity> entityProtos = entities.stream()
                .map(entity -> {
                    if (entity instanceof Job job) {
                        return entityMapper.mapJobToEntityProto(job);
                    } else if (entity instanceof Timelog timelog) {
                        return entityMapper.mapTimelogToEntityProto(timelog);
                    } else if (entity instanceof PaymentLineItem paymentLineItem) {
                        return entityMapper.mapPaymentLineItemToEntityProto(paymentLineItem);
                    } else {
                        return entityMapper.mapToEntityProto(entity);
                    }
                })
                .toList();
        
        return BatchResponse.newBuilder()
                .addAllEntities(entityProtos)
                .putAllErrors(errors)
                .setSuccessCount(entities.size())
                .setFailureCount(errors.size())
                .build();
    }
    
    /**
     * Maps a list of Job entities and error map to a BatchResponse
     */
    default BatchResponse mapJobsToBatchResponse(List<Job> jobs, Map<String, String> errors) {
        EntityMapper entityMapper = EntityMapper.INSTANCE;
        
        List<Entity> entityProtos = jobs.stream()
                .map(entityMapper::mapJobToEntityProto)
                .toList();
        
        return BatchResponse.newBuilder()
                .addAllEntities(entityProtos)
                .putAllErrors(errors)
                .setSuccessCount(jobs.size())
                .setFailureCount(errors.size())
                .build();
    }
    
    /**
     * Maps a list of Timelog entities and error map to a BatchResponse
     */
    default BatchResponse mapTimelogsToBatchResponse(List<Timelog> timelogs, Map<String, String> errors) {
        EntityMapper entityMapper = EntityMapper.INSTANCE;
        
        List<Entity> entityProtos = timelogs.stream()
                .map(entityMapper::mapTimelogToEntityProto)
                .toList();
        
        return BatchResponse.newBuilder()
                .addAllEntities(entityProtos)
                .putAllErrors(errors)
                .setSuccessCount(timelogs.size())
                .setFailureCount(errors.size())
                .build();
    }
    
    /**
     * Maps a list of PaymentLineItem entities and error map to a BatchResponse
     */
    default BatchResponse mapPaymentLineItemsToBatchResponse(List<PaymentLineItem> paymentLineItems, Map<String, String> errors) {
        EntityMapper entityMapper = EntityMapper.INSTANCE;
        
        List<Entity> entityProtos = paymentLineItems.stream()
                .map(entityMapper::mapPaymentLineItemToEntityProto)
                .toList();
        
        return BatchResponse.newBuilder()
                .addAllEntities(entityProtos)
                .putAllErrors(errors)
                .setSuccessCount(paymentLineItems.size())
                .setFailureCount(errors.size())
                .build();
    }
}