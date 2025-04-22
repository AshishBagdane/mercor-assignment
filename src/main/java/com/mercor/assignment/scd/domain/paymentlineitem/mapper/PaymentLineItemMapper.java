package com.mercor.assignment.scd.domain.paymentlineitem.mapper;

import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitems.CreatePaymentLineItemRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemProto;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemResponse;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemListResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * MapStruct mapper for converting between PaymentLineItem entity and protobuf messages
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentLineItemMapper {

    /**
     * Maps a PaymentLineItem entity to PaymentLineItemProto message
     *
     * @param paymentLineItem the entity to map
     * @return the mapped proto message
     */
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "dateToLong")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "dateToLong")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToDouble")
    PaymentLineItemProto toProto(PaymentLineItem paymentLineItem);

    /**
     * Maps a list of PaymentLineItem entities to a PaymentLineItemListResponse
     *
     * @param paymentLineItems the list of entities
     * @return the list response message
     */
    default PaymentLineItemListResponse toListResponse(List<PaymentLineItem> paymentLineItems) {
        return PaymentLineItemListResponse.newBuilder()
                .addAllPaymentLineItems(paymentLineItems.stream()
                        .map(this::toProto)
                        .toList())
                .build();
    }

    /**
     * Maps a PaymentLineItem entity to a PaymentLineItemResponse
     *
     * @param paymentLineItem the entity
     * @return the response message
     */
    default PaymentLineItemResponse toResponse(PaymentLineItem paymentLineItem) {
        return PaymentLineItemResponse.newBuilder()
                .setPaymentLineItem(toProto(paymentLineItem))
                .build();
    }

    /**
     * Maps a PaymentLineItemProto message to a PaymentLineItem entity
     *
     * @param proto the proto message
     * @return the mapped entity
     */
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "longToDate")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "longToDate")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "doubleToDecimal")
    PaymentLineItem toEntity(PaymentLineItemProto proto);

    /**
     * Maps a CreatePaymentLineItemRequest to a PaymentLineItem entity
     *
     * @param request the CreatePaymentLineItemRequest
     * @return the PaymentLineItem entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "doubleToDecimal")
    @Mapping(source = "jobUid", target = "jobUid")
    @Mapping(source = "timelogUid", target = "timelogUid")
    PaymentLineItem toEntity(CreatePaymentLineItemRequest request);

    /**
     * Converts a java.util.Date to a long timestamp
     */
    @Named("dateToLong")
    default Long dateToLong(Date date) {
        return date != null ? date.getTime() : null;
    }

    /**
     * Converts a long timestamp to a java.util.Date
     */
    @Named("longToDate")
    default Date longToDate(Long timestamp) {
        return timestamp != null ? new Date(timestamp) : null;
    }

    /**
     * Converts a BigDecimal to a double
     */
    @Named("bigDecimalToDouble")
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    /**
     * Converts a double to a BigDecimal
     */
    @Named("doubleToDecimal")
    default BigDecimal doubleToDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
