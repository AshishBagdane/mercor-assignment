package com.mercor.assignment.sdc.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for batch operations on SCD entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SCDBatchResponse<T extends SCDEntityDTO> {

  private List<T> entities;
  private Map<String, String> errors;
  private int successCount;
  private int failureCount;
}