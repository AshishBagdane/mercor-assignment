package com.mercor.assignment.sdc.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base DTO class for all SCD entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SCDEntityDTO {

  private String id;
  private Integer version;
  private String uid;
  private Date createdAt;
  private Date updatedAt;
}
