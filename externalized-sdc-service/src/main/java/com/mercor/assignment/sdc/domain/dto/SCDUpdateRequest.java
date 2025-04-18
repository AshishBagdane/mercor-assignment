package com.mercor.assignment.sdc.domain.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating SCD entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SCDUpdateRequest<T extends SCDEntityDTO> {

  @Builder.Default
  private Map<String, Object> fields = new HashMap<>();

  private T entity;

  public void addField(String key, Object value) {
    fields.put(key, value);
  }
}