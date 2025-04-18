package com.mercor.assignment.sdc.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for batch operations on SCD entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SCDBatchRequest<T extends SCDEntityDTO> {

  private List<String> ids;
  private List<T> entities;
  private Map<String, Object> commonFields;
}