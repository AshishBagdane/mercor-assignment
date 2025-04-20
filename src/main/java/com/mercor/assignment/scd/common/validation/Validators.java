package com.mercor.assignment.scd.common.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A utility class providing validators using lambda expressions for various data types
 */
public class Validators {

    // Common validator interface that works with any type
    @FunctionalInterface
    public interface Validator<T> {
        boolean isValid(T value);
        
        // Utility method to combine validators with AND logic
        default Validator<T> and(Validator<T> other) {
            return value -> this.isValid(value) && other.isValid(value);
        }
        
        // Utility method to combine validators with OR logic
        default Validator<T> or(Validator<T> other) {
            return value -> this.isValid(value) || other.isValid(value);
        }
        
        // Utility method to negate a validator
        default Validator<T> not() {
            return value -> !this.isValid(value);
        }
    }
    
    /**
     * Validators for String values
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StringValidators {
        // Common String validators
        public static final Validator<String> notNull = Objects::nonNull;
        public static final Validator<String> notEmpty = value -> value != null && !value.isEmpty();
        public static final Validator<String> notBlank = value -> value != null && !value.trim().isEmpty();
        
        // Specific String validators
        public static Validator<String> hasMinLength(int minLength) {
            return value -> value != null && value.length() >= minLength;
        }
        
        public static Validator<String> hasMaxLength(int maxLength) {
            return value -> value != null && value.length() <= maxLength;
        }
        
        public static Validator<String> hasLengthBetween(int minLength, int maxLength) {
            return hasMinLength(minLength).and(hasMaxLength(maxLength));
        }
        
        public static Validator<String> matchesPattern(String regex) {
            Pattern pattern = Pattern.compile(regex);
            return value -> value != null && pattern.matcher(value).matches();
        }
        
        public static final Validator<String> isAlphanumeric = matchesPattern("^[a-zA-Z0-9]*$");
        
        public static final Validator<String> containsUppercase = value -> 
            value != null && value.chars().anyMatch(Character::isUpperCase);
            
        public static final Validator<String> containsLowercase = value -> 
            value != null && value.chars().anyMatch(Character::isLowerCase);
            
        public static final Validator<String> containsDigit = value -> 
            value != null && value.chars().anyMatch(Character::isDigit);
            
        public static Validator<String> containsSpecialChar(String specialChars) {
            return value -> value != null && value.chars().anyMatch(ch -> specialChars.indexOf(ch) >= 0);
        }
    }
    
    /**
     * Validators for Double values
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DoubleValidators {
        // Common Double validators
        public static final Validator<Double> notNull = value -> value != null;
        public static final Validator<Double> isPositive = value -> value != null && value > 0;
        public static final Validator<Double> isNonNegative = value -> value != null && value >= 0;
        public static final Validator<Double> isFinite = value -> value != null && Double.isFinite(value);
        
        // Specific Double validators
        public static Validator<Double> greaterThan(double min) {
            return value -> value != null && value > min;
        }
        
        public static Validator<Double> lessThan(double max) {
            return value -> value != null && value < max;
        }
        
        public static Validator<Double> between(double min, double max) {
            return greaterThan(min).and(lessThan(max));
        }
        
        public static Validator<Double> equalsTo(double target) {
            return value -> value != null && Math.abs(value - target) < 0.000001; // Using epsilon for float comparison
        }

        public static Validator<Double> hasMaxDecimalPlaces(int maxPlaces) {
            return value -> {
                if (value == null) return false;
                String stringValue = Double.toString(value);
                int decimalPos = stringValue.indexOf('.');
                // Handle scientific notation (e.g., 1.23E-10)
                int ePos = stringValue.toUpperCase().indexOf('E');
                if (ePos != -1) {
                    // For scientific notation, just check decimals before the 'E'
                    return decimalPos == -1 || (ePos - decimalPos - 1) <= maxPlaces;
                }
                return decimalPos == -1 || (stringValue.length() - decimalPos - 1) <= maxPlaces;
            };
        }
    }
    
    /**
     * Validators for BigDecimal values
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BigDecimalValidators {
        // Common BigDecimal validators
        public static final Validator<BigDecimal> notNull = value -> value != null;
        public static final Validator<BigDecimal> isPositive = value -> 
            value != null && value.compareTo(BigDecimal.ZERO) > 0;
        public static final Validator<BigDecimal> isNonNegative = value -> 
            value != null && value.compareTo(BigDecimal.ZERO) >= 0;
        
        // Specific BigDecimal validators
        public static Validator<BigDecimal> greaterThan(BigDecimal min) {
            return value -> value != null && value.compareTo(min) > 0;
        }
        
        public static Validator<BigDecimal> lessThan(BigDecimal max) {
            return value -> value != null && value.compareTo(max) < 0;
        }
        
        public static Validator<BigDecimal> between(BigDecimal min, BigDecimal max) {
            return greaterThan(min).and(lessThan(max));
        }
        
        public static Validator<BigDecimal> equalsTo(BigDecimal target) {
            return value -> value != null && value.compareTo(target) == 0;
        }
        
        public static Validator<BigDecimal> hasScale(int scale) {
            return value -> value != null && value.scale() == scale;
        }
        
        public static Validator<BigDecimal> hasMaxScale(int maxScale) {
            return value -> value != null && value.scale() <= maxScale;
        }

        public static Validator<BigDecimal> hasPrecision(int precision) {
            return value -> {
                if (value == null) return false;
                // Remove trailing zeros to get the actual precision
                BigDecimal stripped = value.stripTrailingZeros();
                // Get the precision by counting all digits regardless of decimal point
                int actualPrecision = stripped.precision();
                return actualPrecision <= precision;
            };
        }
    }
    
    /**
     * A validator aggregator that collects validation errors
     */
    public static class ValidationResult<T> {
        private final T value;
        private final List<String> errors = new ArrayList<>();
        
        public ValidationResult(T value) {
            this.value = value;
        }
        
        public ValidationResult<T> validate(Validator<T> validator, String errorMessage) {
            if (!validator.isValid(value)) {
                errors.add(errorMessage);
            }
            return this;
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public T getValue() {
            return value;
        }
    }
}