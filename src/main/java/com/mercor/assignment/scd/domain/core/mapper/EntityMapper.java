package com.mercor.assignment.scd.domain.core.mapper;

import com.google.protobuf.ByteString;
import com.mercor.assignment.scd.domain.common.Entity;
import com.mercor.assignment.scd.domain.core.EntityListResponse;
import com.mercor.assignment.scd.domain.core.EntityResponse;
import com.mercor.assignment.scd.domain.core.model.SCDEntity;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for converting between SCD entities and Protocol Buffer messages
 */
@Mapper
public interface EntityMapper {

    EntityMapper INSTANCE = Mappers.getMapper(EntityMapper.class);

    /**
     * Maps a generic SCDEntity to the common Entity proto message
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "type", constant = "generic")
//    @Mapping(target = "data", expression = "java(serializeEntityData(entity))")
    Entity mapToEntityProto(SCDEntity entity);

    /**
     * Maps a Job entity to the common Entity proto message with Job-specific data
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "type", constant = "jobs")
    @Mapping(target = "data", expression = "java(serializeJobData(job))")
    Entity mapJobToEntityProto(Job job);

    /**
     * Maps a Timelog entity to the common Entity proto message with Timelog-specific data
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "type", constant = "timelog")
//    @Mapping(target = "data", expression = "java(serializeTimelogData(timelog))")
    Entity mapTimelogToEntityProto(Timelog timelog);

    /**
     * Maps a PaymentLineItem entity to the common Entity proto message with PaymentLineItem-specific data
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "dateToMillis")
    @Mapping(target = "type", constant = "payment_line_items")
//    @Mapping(target = "data", expression = "java(serializePaymentLineItemData(paymentLineItem))")
    Entity mapPaymentLineItemToEntityProto(PaymentLineItem paymentLineItem);

    /**
     * Wraps an Entity proto in an EntityResponse
     */
    default EntityResponse wrapInEntityResponse(Entity entity) {
        return EntityResponse.newBuilder().setEntity(entity).build();
    }

    /**
     * Creates an EntityListResponse from a list of Entity protos
     */
    default EntityListResponse wrapInEntityListResponse(List<Entity> entities) {
        return EntityListResponse.newBuilder().addAllEntities(entities).build();
    }

    /**
     * Maps a Job entity to an EntityResponse
     */
    default EntityResponse mapJobToEntityResponse(Job job) {
        return wrapInEntityResponse(mapJobToEntityProto(job));
    }

    /**
     * Maps a Timelog entity to an EntityResponse
     */
    default EntityResponse mapTimelogToEntityResponse(Timelog timelog) {
        return wrapInEntityResponse(mapTimelogToEntityProto(timelog));
    }

    /**
     * Maps a PaymentLineItem entity to an EntityResponse
     */
    default EntityResponse mapPaymentLineItemToEntityResponse(PaymentLineItem paymentLineItem) {
        return wrapInEntityResponse(mapPaymentLineItemToEntityProto(paymentLineItem));
    }

    /**
     * Maps a list of Job entities to an EntityListResponse
     */
    default EntityListResponse mapJobsToEntityListResponse(List<Job> jobs) {
        List<Entity> entityProtos = jobs.stream()
            .map(this::mapJobToEntityProto)
            .toList();
        return wrapInEntityListResponse(entityProtos);
    }

    /**
     * Maps a list of Timelog entities to an EntityListResponse
     */
    default EntityListResponse mapTimelogsToEntityListResponse(List<Timelog> timelogs) {
        List<Entity> entityProtos = timelogs.stream()
            .map(this::mapTimelogToEntityProto)
            .toList();
        return wrapInEntityListResponse(entityProtos);
    }

    /**
     * Maps a list of PaymentLineItem entities to an EntityListResponse
     */
    default EntityListResponse mapPaymentLineItemsToEntityListResponse(List<PaymentLineItem> paymentLineItems) {
        List<Entity> entityProtos = paymentLineItems.stream()
            .map(this::mapPaymentLineItemToEntityProto)
            .toList();
        return wrapInEntityListResponse(entityProtos);
    }

    /**
     * Serializes generic entity data to ByteString
     */
    default ByteString serializeEntityData(SCDEntity entity) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(entity);
            return ByteString.copyFrom(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing entity data", e);
        }
    }

    /**
     * Serializes Job-specific data to ByteString
     */
    default ByteString serializeJobData(Job job) {
        try {
            // Create a map of job properties to serialize
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("status", job.getStatus());
            jobData.put("rate", job.getRate());
            jobData.put("title", job.getTitle());
            jobData.put("companyId", job.getCompanyId());
            jobData.put("contractorId", job.getContractorId());

            // Serialize the map
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(jobData);
            oos.close();

            return ByteString.copyFrom(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing job data", e);
        }
    }

    /**
     * Serializes Timelog-specific data to ByteString
     */
    default ByteString serializeTimelogData(Timelog timelog) {
        try {
            // Create a map of timelog properties to serialize
            Map<String, Object> timelogData = new HashMap<>();
            timelogData.put("duration", timelog.getDuration());
            timelogData.put("timeStart", timelog.getTimeStart());
            timelogData.put("timeEnd", timelog.getTimeEnd());
            timelogData.put("type", timelog.getType());
            timelogData.put("jobUid", timelog.getJobUid());

            // Serialize the map
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(timelogData);
            oos.close();

            return ByteString.copyFrom(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing timelog data", e);
        }
    }

    /**
     * Serializes PaymentLineItem-specific data to ByteString
     */
    default ByteString serializePaymentLineItemData(PaymentLineItem paymentLineItem) {
        try {
            // Create a map of payment line item properties to serialize
            Map<String, Object> paymentLineItemData = new HashMap<>();
            paymentLineItemData.put("jobUid", paymentLineItem.getJobUid());
            paymentLineItemData.put("timelogUid", paymentLineItem.getTimelogUid());
            paymentLineItemData.put("amount", paymentLineItem.getAmount());
            paymentLineItemData.put("status", paymentLineItem.getStatus());

            // Serialize the map
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(paymentLineItemData);
            oos.close();

            return ByteString.copyFrom(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error serializing payment line item data", e);
        }
    }

    /**
     * Converts a Java Date to milliseconds since epoch
     */
    @Named("dateToMillis")
    default long dateToMillis(Date date) {
        if (date == null) {
            return 0L;
        }
        return date.getTime();
    }
}