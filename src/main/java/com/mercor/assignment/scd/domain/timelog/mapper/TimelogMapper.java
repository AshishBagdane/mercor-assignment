package com.mercor.assignment.scd.domain.timelog.mapper;

import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitems.CreatePaymentLineItemRequest;
import com.mercor.assignment.scd.domain.timelog.CreateNewTimelogForJobRequest;
import com.mercor.assignment.scd.domain.timelog.TimelogProto;
import com.mercor.assignment.scd.domain.timelog.AdjustTimelogRequest;
import com.mercor.assignment.scd.domain.timelog.TimelogResponse;
import com.mercor.assignment.scd.domain.timelog.TimelogListResponse;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Date;
import java.util.List;

/**
 * MapStruct mapper for converting between Timelog entity and proto objects
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelogMapper {

    TimelogMapper INSTANCE = Mappers.getMapper(TimelogMapper.class);

    /**
     * Maps a Timelog entity to a Timelog proto object
     *
     * @param entity The Timelog entity to map
     * @return The mapped Timelog proto object
     */
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "dateToLong")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "dateToLong")
    TimelogProto toProto(Timelog entity);

    /**
     * Maps a Timelog proto object to a Timelog entity
     *
     * @param proto The Timelog proto object to map
     * @return The mapped Timelog entity
     */
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "longToDate")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "longToDate")
    Timelog toEntity(TimelogProto proto);

    /**
     * Maps a CreateNewTimelogForJobRequest to a Timelog entity
     *
     * @param request the CreateNewTimelogForJobRequest
     * @return the Timelog entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "type", target = "type")
    @Mapping(source = "duration", target = "duration")
    @Mapping(source = "startTime", target = "timeStart")
    @Mapping(source = "endTime", target = "timeEnd")
    @Mapping(source = "jobUid", target = "jobUid")
    Timelog toEntity(CreateNewTimelogForJobRequest request);

    /**
     * Maps a list of Timelog entities to a TimelogListResponse
     *
     * @param entities The list of Timelog entities to map
     * @return The mapped TimelogListResponse
     */
    default TimelogListResponse toTimelogListResponse(List<Timelog> entities) {
        if (entities == null) {
            return TimelogListResponse.newBuilder().build();
        }

        TimelogListResponse.Builder builder = TimelogListResponse.newBuilder();
        entities.forEach(entity -> builder.addTimelogs(toProto(entity)));
        return builder.build();
    }

    /**
     * Maps a Timelog entity to a TimelogResponse
     *
     * @param entity The Timelog entity to map
     * @return The mapped TimelogResponse
     */
    default TimelogResponse toTimelogResponse(Timelog entity) {
        if (entity == null) {
            return TimelogResponse.newBuilder().build();
        }

        return TimelogResponse.newBuilder()
                .setTimelog(toProto(entity))
                .build();
    }

    /**
     * Updates a Timelog entity from an AdjustTimelogRequest
     *
     * @param request The AdjustTimelogRequest to map from
     * @param entity The Timelog entity to update
     * @return The updated Timelog entity
     */
    @Mapping(source = "adjustedDuration", target = "duration")
    @Mapping(target = "type", constant = "adjusted")
    Timelog updateEntityFromRequest(AdjustTimelogRequest request, @MappingTarget Timelog entity);

    /**
     * Converts a Date to a long timestamp
     *
     * @param date The Date to convert
     * @return The timestamp as a long
     */
    @Named("dateToLong")
    default Long dateToLong(Date date) {
        return date != null ? date.getTime() : null;
    }

    /**
     * Converts a long timestamp to a Date
     *
     * @param timestamp The timestamp to convert
     * @return The Date
     */
    @Named("longToDate")
    default Date longToDate(Long timestamp) {
        return timestamp != null ? new Date(timestamp) : null;
    }
}
