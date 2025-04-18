package com.mercor.assignment.sdc.domain.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for querying SCD entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SCDQueryRequest {

  @Builder.Default
  private Map<String, Object> conditions = new HashMap<>();

  @Builder.Default
  private boolean latestVersionOnly = true;

  private Integer limit;
  private Integer offset;
  private String sortBy;
  private String sortDirection;

  public void addCondition(String key, Object value) {
    conditions.put(key, value);
  }

  /**
   * Get the sort field in camelCase format for JPA queries
   * 
   * @return The sortBy field in camelCase format
   */
  public String getSortByFieldForJpa() {
    if (sortBy == null || sortBy.isEmpty()) {
      return null;
    }

    // Convert snake_case to camelCase if needed
    if (sortBy.contains("_")) {
      StringBuilder camelCase = new StringBuilder();
      boolean nextUpper = false;

      for (char c : sortBy.toCharArray()) {
        if (c == '_') {
          nextUpper = true;
        } else {
          camelCase.append(nextUpper ? Character.toUpperCase(c) : c);
          nextUpper = false;
        }
      }

      return camelCase.toString();
    }

    return sortBy;
  }

  /**
   * Parse a field key to extract field name and operator
   * Example: "rate >" will return ["rate", ">"]
   * 
   * @param conditionKey The condition key to parse
   * @return An array with the field name and operator (or null if no operator)
   */
  public static String[] parseConditionKey(String conditionKey) {
    if (conditionKey == null || conditionKey.isEmpty()) {
      return new String[] { null, null };
    }

    // Check for common operators
    if (conditionKey.contains(" > ")) {
      String[] parts = conditionKey.split(" > ", 2);
      return new String[] { parts[0].trim(), ">" };
    } else if (conditionKey.contains(" < ")) {
      String[] parts = conditionKey.split(" < ", 2);
      return new String[] { parts[0].trim(), "<" };
    } else if (conditionKey.contains(" >= ")) {
      String[] parts = conditionKey.split(" >= ", 2);
      return new String[] { parts[0].trim(), ">=" };
    } else if (conditionKey.contains(" <= ")) {
      String[] parts = conditionKey.split(" <= ", 2);
      return new String[] { parts[0].trim(), "<=" };
    } else if (conditionKey.contains(" = ")) {
      String[] parts = conditionKey.split(" = ", 2);
      return new String[] { parts[0].trim(), "=" };
    } else if (conditionKey.contains(" != ")) {
      String[] parts = conditionKey.split(" != ", 2);
      return new String[] { parts[0].trim(), "!=" };
    } else if (conditionKey.contains(" LIKE ")) {
      String[] parts = conditionKey.split(" LIKE ", 2);
      return new String[] { parts[0].trim(), "LIKE" };
    }

    // If no operator is found, assume equality
    return new String[] { conditionKey.trim(), "=" };
  }

  /**
   * Convert a snake_case field name to camelCase
   * 
   * @param fieldName The field name in snake_case
   * @return The field name in camelCase
   */
  public static String toCamelCase(String fieldName) {
    if (fieldName == null || fieldName.isEmpty() || !fieldName.contains("_")) {
      return fieldName;
    }

    StringBuilder camelCase = new StringBuilder();
    boolean nextUpper = false;

    for (char c : fieldName.toCharArray()) {
      if (c == '_') {
        nextUpper = true;
      } else {
        camelCase.append(nextUpper ? Character.toUpperCase(c) : c);
        nextUpper = false;
      }
    }

    return camelCase.toString();
  }
}
