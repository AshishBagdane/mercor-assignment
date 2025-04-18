package com.mercor.assignment.sdc.service.mapper;

import com.mercor.assignment.sdc.domain.dto.PaymentLineItemDTO;
import com.mercor.assignment.sdc.domain.entity.PaymentLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for converting between PaymentLineItem entity and PaymentLineItemDTO Using MapStruct for automatic mapping implementation
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentLineItemMapper extends SCDMapper<PaymentLineItemDTO, PaymentLineItem> {

  /**
   * Convert a PaymentLineItem entity to a PaymentLineItemDTO
   *
   * @param entity The PaymentLineItem entity to convert
   * @return The corresponding PaymentLineItemDTO
   */
  @Override
  PaymentLineItemDTO toDto(PaymentLineItem entity);

  /**
   * Convert a PaymentLineItemDTO to a PaymentLineItem entity
   *
   * @param dto The PaymentLineItemDTO to convert
   * @return The corresponding PaymentLineItem entity
   */
  @Override
  PaymentLineItem toEntity(PaymentLineItemDTO dto);

  /**
   * Update a PaymentLineItem entity with data from another PaymentLineItem entity
   *
   * @param sourceEntity The source entity containing updated data
   * @param targetEntity The target entity to update
   */
  @Override
  void updateEntityFromDto(@MappingTarget PaymentLineItem targetEntity, PaymentLineItem sourceEntity);
}