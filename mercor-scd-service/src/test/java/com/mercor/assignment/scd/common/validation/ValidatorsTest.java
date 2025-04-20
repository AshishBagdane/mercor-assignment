package com.mercor.assignment.scd.common.validation;

import com.mercor.assignment.scd.common.validation.Validators.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidatorsTest {

    // String Validators Tests
    @Test
    void testStringNotNull() {
        assertTrue(Validators.StringValidators.notNull.isValid("test"));
        assertFalse(Validators.StringValidators.notNull.isValid(null));
    }

    @Test
    void testStringNotEmpty() {
        assertTrue(Validators.StringValidators.notEmpty.isValid("test"));
        assertFalse(Validators.StringValidators.notEmpty.isValid(""));
        assertFalse(Validators.StringValidators.notEmpty.isValid(null));
    }

    @Test
    void testStringNotBlank() {
        assertTrue(Validators.StringValidators.notBlank.isValid("test"));
        assertFalse(Validators.StringValidators.notBlank.isValid("  "));
        assertFalse(Validators.StringValidators.notBlank.isValid(""));
        assertFalse(Validators.StringValidators.notBlank.isValid(null));
    }

    @Test
    void testStringHasMinLength() {
        Validators.Validator<String> minLength5 = Validators.StringValidators.hasMinLength(5);
        assertTrue(minLength5.isValid("12345"));
        assertTrue(minLength5.isValid("123456"));
        assertFalse(minLength5.isValid("1234"));
        assertFalse(minLength5.isValid(""));
        assertFalse(minLength5.isValid(null));
    }

    @Test
    void testStringHasMaxLength() {
        Validators.Validator<String> maxLength5 = Validators.StringValidators.hasMaxLength(5);
        assertTrue(maxLength5.isValid("12345"));
        assertTrue(maxLength5.isValid("1234"));
        assertTrue(maxLength5.isValid(""));
        assertFalse(maxLength5.isValid("123456"));
        assertFalse(maxLength5.isValid(null));
    }

    @Test
    void testStringHasLengthBetween() {
        Validators.Validator<String> lengthBetween3And5 = Validators.StringValidators.hasLengthBetween(3, 5);
        assertTrue(lengthBetween3And5.isValid("123"));
        assertTrue(lengthBetween3And5.isValid("1234"));
        assertTrue(lengthBetween3And5.isValid("12345"));
        assertFalse(lengthBetween3And5.isValid("12"));
        assertFalse(lengthBetween3And5.isValid("123456"));
        assertFalse(lengthBetween3And5.isValid(""));
        assertFalse(lengthBetween3And5.isValid(null));
    }

    @Test
    void testStringMatchesPattern() {
        Validators.Validator<String> digitOnly = Validators.StringValidators.matchesPattern("\\d+");
        assertTrue(digitOnly.isValid("123"));
        assertFalse(digitOnly.isValid("abc"));
        assertFalse(digitOnly.isValid("123abc"));
        assertFalse(digitOnly.isValid(""));
        assertFalse(digitOnly.isValid(null));
    }

    @Test
    void testIsAlphanumeric() {
        assertTrue(Validators.StringValidators.isAlphanumeric.isValid("abc123"));
        assertTrue(Validators.StringValidators.isAlphanumeric.isValid("ABC123"));
        assertTrue(Validators.StringValidators.isAlphanumeric.isValid(""));
        assertFalse(Validators.StringValidators.isAlphanumeric.isValid("abc-123"));
        assertFalse(Validators.StringValidators.isAlphanumeric.isValid("abc 123"));
        assertFalse(Validators.StringValidators.isAlphanumeric.isValid(null));
    }

    @Test
    void testContainsUppercase() {
        assertTrue(Validators.StringValidators.containsUppercase.isValid("abcD"));
        assertTrue(Validators.StringValidators.containsUppercase.isValid("ABCD"));
        assertFalse(Validators.StringValidators.containsUppercase.isValid("abcd"));
        assertFalse(Validators.StringValidators.containsUppercase.isValid("123"));
        assertFalse(Validators.StringValidators.containsUppercase.isValid(""));
        assertFalse(Validators.StringValidators.containsUppercase.isValid(null));
    }

    @Test
    void testContainsLowercase() {
        assertTrue(Validators.StringValidators.containsLowercase.isValid("ABCd"));
        assertTrue(Validators.StringValidators.containsLowercase.isValid("abcd"));
        assertFalse(Validators.StringValidators.containsLowercase.isValid("ABCD"));
        assertFalse(Validators.StringValidators.containsLowercase.isValid("123"));
        assertFalse(Validators.StringValidators.containsLowercase.isValid(""));
        assertFalse(Validators.StringValidators.containsLowercase.isValid(null));
    }

    @Test
    void testContainsDigit() {
        assertTrue(Validators.StringValidators.containsDigit.isValid("abc1"));
        assertTrue(Validators.StringValidators.containsDigit.isValid("123"));
        assertFalse(Validators.StringValidators.containsDigit.isValid("abcd"));
        assertFalse(Validators.StringValidators.containsDigit.isValid(""));
        assertFalse(Validators.StringValidators.containsDigit.isValid(null));
    }

    @Test
    void testContainsSpecialChar() {
        Validators.Validator<String> containsSpecial = Validators.StringValidators.containsSpecialChar("!@#");
        assertTrue(containsSpecial.isValid("abc!"));
        assertTrue(containsSpecial.isValid("@abc"));
        assertTrue(containsSpecial.isValid("abc#def"));
        assertFalse(containsSpecial.isValid("abc$"));
        assertFalse(containsSpecial.isValid("abcd"));
        assertFalse(containsSpecial.isValid(""));
        assertFalse(containsSpecial.isValid(null));
    }

    // Double Validators Tests
    @Test
    void testDoubleNotNull() {
        assertTrue(Validators.DoubleValidators.notNull.isValid(0.0));
        assertTrue(Validators.DoubleValidators.notNull.isValid(-1.0));
        assertFalse(Validators.DoubleValidators.notNull.isValid(null));
    }

    @Test
    void testDoubleIsPositive() {
        assertTrue(Validators.DoubleValidators.isPositive.isValid(1.0));
        assertTrue(Validators.DoubleValidators.isPositive.isValid(0.1));
        assertFalse(Validators.DoubleValidators.isPositive.isValid(0.0));
        assertFalse(Validators.DoubleValidators.isPositive.isValid(-1.0));
        assertFalse(Validators.DoubleValidators.isPositive.isValid(null));
    }

    @Test
    void testDoubleIsNonNegative() {
        assertTrue(Validators.DoubleValidators.isNonNegative.isValid(0.0));
        assertTrue(Validators.DoubleValidators.isNonNegative.isValid(1.0));
        assertFalse(Validators.DoubleValidators.isNonNegative.isValid(-0.1));
        assertFalse(Validators.DoubleValidators.isNonNegative.isValid(null));
    }

    @Test
    void testDoubleIsFinite() {
        assertTrue(Validators.DoubleValidators.isFinite.isValid(0.0));
        assertTrue(Validators.DoubleValidators.isFinite.isValid(Double.MAX_VALUE));
        assertTrue(Validators.DoubleValidators.isFinite.isValid(Double.MIN_VALUE));
        assertFalse(Validators.DoubleValidators.isFinite.isValid(Double.POSITIVE_INFINITY));
        assertFalse(Validators.DoubleValidators.isFinite.isValid(Double.NEGATIVE_INFINITY));
        assertFalse(Validators.DoubleValidators.isFinite.isValid(Double.NaN));
        assertFalse(Validators.DoubleValidators.isFinite.isValid(null));
    }

    @Test
    void testDoubleGreaterThan() {
        Validators.Validator<Double> greaterThan5 = Validators.DoubleValidators.greaterThan(5.0);
        assertTrue(greaterThan5.isValid(5.1));
        assertTrue(greaterThan5.isValid(6.0));
        assertFalse(greaterThan5.isValid(5.0));
        assertFalse(greaterThan5.isValid(4.9));
        assertFalse(greaterThan5.isValid(null));
    }

    @Test
    void testDoubleLessThan() {
        Validators.Validator<Double> lessThan5 = Validators.DoubleValidators.lessThan(5.0);
        assertTrue(lessThan5.isValid(4.9));
        assertTrue(lessThan5.isValid(0.0));
        assertFalse(lessThan5.isValid(5.0));
        assertFalse(lessThan5.isValid(5.1));
        assertFalse(lessThan5.isValid(null));
    }

    @Test
    void testDoubleBetween() {
        Validators.Validator<Double> between0And10 = Validators.DoubleValidators.between(0.0, 10.0);
        assertTrue(between0And10.isValid(0.1));
        assertTrue(between0And10.isValid(5.0));
        assertTrue(between0And10.isValid(9.9));
        assertFalse(between0And10.isValid(0.0));
        assertFalse(between0And10.isValid(10.0));
        assertFalse(between0And10.isValid(-0.1));
        assertFalse(between0And10.isValid(10.1));
        assertFalse(between0And10.isValid(null));
    }

    @Test
    void testDoubleEqualsTo() {
        Validators.Validator<Double> equals5 = Validators.DoubleValidators.equalsTo(5.0);
        assertTrue(equals5.isValid(5.0));
        // Test with epsilon for floating point comparison
        assertTrue(equals5.isValid(5.0000001));
        assertFalse(equals5.isValid(5.1));
        assertFalse(equals5.isValid(4.9));
        assertFalse(equals5.isValid(null));
    }

    @Test
    void testDoubleHasMaxDecimalPlaces() {
        Validators.Validator<Double> maxTwoDecimalPlaces = Validators.DoubleValidators.hasMaxDecimalPlaces(2);
        assertTrue(maxTwoDecimalPlaces.isValid(5.0));
        assertTrue(maxTwoDecimalPlaces.isValid(5.1));
        assertTrue(maxTwoDecimalPlaces.isValid(5.12));
        assertFalse(maxTwoDecimalPlaces.isValid(5.123));
        assertFalse(maxTwoDecimalPlaces.isValid(null));
        
        // Edge case: very small number in scientific notation
        assertTrue(maxTwoDecimalPlaces.isValid(1.23e-10)); // This might be represented as "1.23E-10" with only 2 decimal places
    }

    // BigDecimal Validators Tests
    @Test
    void testBigDecimalNotNull() {
        assertTrue(Validators.BigDecimalValidators.notNull.isValid(BigDecimal.ZERO));
        assertTrue(Validators.BigDecimalValidators.notNull.isValid(BigDecimal.ONE));
        assertFalse(Validators.BigDecimalValidators.notNull.isValid(null));
    }

    @Test
    void testBigDecimalIsPositive() {
        assertTrue(Validators.BigDecimalValidators.isPositive.isValid(BigDecimal.ONE));
        assertTrue(Validators.BigDecimalValidators.isPositive.isValid(new BigDecimal("0.1")));
        assertFalse(Validators.BigDecimalValidators.isPositive.isValid(BigDecimal.ZERO));
        assertFalse(Validators.BigDecimalValidators.isPositive.isValid(new BigDecimal("-0.1")));
        assertFalse(Validators.BigDecimalValidators.isPositive.isValid(null));
    }

    @Test
    void testBigDecimalIsNonNegative() {
        assertTrue(Validators.BigDecimalValidators.isNonNegative.isValid(BigDecimal.ZERO));
        assertTrue(Validators.BigDecimalValidators.isNonNegative.isValid(BigDecimal.ONE));
        assertFalse(Validators.BigDecimalValidators.isNonNegative.isValid(new BigDecimal("-0.1")));
        assertFalse(Validators.BigDecimalValidators.isNonNegative.isValid(null));
    }

    @Test
    void testBigDecimalGreaterThan() {
        Validators.Validator<BigDecimal> greaterThan5 = Validators.BigDecimalValidators.greaterThan(new BigDecimal("5"));
        assertTrue(greaterThan5.isValid(new BigDecimal("5.1")));
        assertTrue(greaterThan5.isValid(new BigDecimal("6")));
        assertFalse(greaterThan5.isValid(new BigDecimal("5")));
        assertFalse(greaterThan5.isValid(new BigDecimal("4.9")));
        assertFalse(greaterThan5.isValid(null));
    }

    @Test
    void testBigDecimalLessThan() {
        Validators.Validator<BigDecimal> lessThan5 = Validators.BigDecimalValidators.lessThan(new BigDecimal("5"));
        assertTrue(lessThan5.isValid(new BigDecimal("4.9")));
        assertTrue(lessThan5.isValid(BigDecimal.ZERO));
        assertFalse(lessThan5.isValid(new BigDecimal("5")));
        assertFalse(lessThan5.isValid(new BigDecimal("5.1")));
        assertFalse(lessThan5.isValid(null));
    }

    @Test
    void testBigDecimalBetween() {
        Validators.Validator<BigDecimal> between0And10 = Validators.BigDecimalValidators.between(
                BigDecimal.ZERO, new BigDecimal("10"));
        assertTrue(between0And10.isValid(new BigDecimal("0.1")));
        assertTrue(between0And10.isValid(new BigDecimal("5")));
        assertTrue(between0And10.isValid(new BigDecimal("9.9")));
        assertFalse(between0And10.isValid(BigDecimal.ZERO));
        assertFalse(between0And10.isValid(new BigDecimal("10")));
        assertFalse(between0And10.isValid(new BigDecimal("-0.1")));
        assertFalse(between0And10.isValid(new BigDecimal("10.1")));
        assertFalse(between0And10.isValid(null));
    }

    @Test
    void testBigDecimalEqualsTo() {
        Validators.Validator<BigDecimal> equals5 = Validators.BigDecimalValidators.equalsTo(new BigDecimal("5"));
        assertTrue(equals5.isValid(new BigDecimal("5")));
        assertTrue(equals5.isValid(new BigDecimal("5.0"))); // Same numeric value, different scale
        assertFalse(equals5.isValid(new BigDecimal("5.1")));
        assertFalse(equals5.isValid(new BigDecimal("4.9")));
        assertFalse(equals5.isValid(null));
    }

    @Test
    void testBigDecimalHasScale() {
        Validators.Validator<BigDecimal> scale2 = Validators.BigDecimalValidators.hasScale(2);
        assertTrue(scale2.isValid(new BigDecimal("5.12")));
        assertTrue(scale2.isValid(new BigDecimal("0.00")));
        assertFalse(scale2.isValid(new BigDecimal("5.1")));
        assertFalse(scale2.isValid(new BigDecimal("5")));
        assertFalse(scale2.isValid(new BigDecimal("5.123")));
        assertFalse(scale2.isValid(null));
    }

    @Test
    void testBigDecimalHasMaxScale() {
        Validators.Validator<BigDecimal> maxScale2 = Validators.BigDecimalValidators.hasMaxScale(2);
        assertTrue(maxScale2.isValid(new BigDecimal("5.12")));
        assertTrue(maxScale2.isValid(new BigDecimal("5.1")));
        assertTrue(maxScale2.isValid(new BigDecimal("5")));
        assertFalse(maxScale2.isValid(new BigDecimal("5.123")));
        assertFalse(maxScale2.isValid(null));
    }

    @Test
    void testBigDecimalHasPrecision() {
        Validators.Validator<BigDecimal> precision3 = Validators.BigDecimalValidators.hasPrecision(3);
        assertTrue(precision3.isValid(new BigDecimal("123")));
        assertTrue(precision3.isValid(new BigDecimal("12.3")));
        assertTrue(precision3.isValid(new BigDecimal("1.23")));
        assertTrue(precision3.isValid(new BigDecimal("0.123")));
        assertFalse(precision3.isValid(new BigDecimal("1234")));
        assertFalse(precision3.isValid(new BigDecimal("12.34")));
        assertFalse(precision3.isValid(null));
        
        // Test with trailing zeros
        assertTrue(precision3.isValid(new BigDecimal("12.0"))); // Parsed as "12" with precision 2
        assertTrue(precision3.isValid(new BigDecimal("1.20"))); // Parsed as "12" with precision 2
    }

    // ValidationResult Tests
    @Test
    void testValidationResultWithValidData() {
        String testData = "ValidData";
        ValidationResult<String> result = new ValidationResult<>(testData)
                .validate(Validators.StringValidators.notEmpty, "Cannot be empty")
                .validate(Validators.StringValidators.hasMinLength(3), "Must be at least 3 characters");
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(testData, result.getValue());
    }

    @Test
    void testValidationResultWithInvalidData() {
        String testData = ""; // Empty string will fail validation
        ValidationResult<String> result = new ValidationResult<>(testData)
                .validate(Validators.StringValidators.notEmpty, "Cannot be empty")
                .validate(Validators.StringValidators.hasMinLength(3), "Must be at least 3 characters");
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("Cannot be empty"));
        assertTrue(result.getErrors().contains("Must be at least 3 characters"));
        assertEquals(testData, result.getValue());
    }

    @Test
    void testValidationResultWithMixedValidation() {
        String testData = "A"; // Passes notEmpty but fails hasMinLength(3)
        ValidationResult<String> result = new ValidationResult<>(testData)
                .validate(Validators.StringValidators.notEmpty, "Cannot be empty")
                .validate(Validators.StringValidators.hasMinLength(3), "Must be at least 3 characters");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().contains("Must be at least 3 characters"));
        assertEquals(testData, result.getValue());
    }

    // Validator Combination Tests
    @Test
    void testValidatorAnd() {
        Validators.Validator<String> notEmptyAndAlphanumeric = Validators.StringValidators.notEmpty
                .and(Validators.StringValidators.isAlphanumeric);
        
        assertTrue(notEmptyAndAlphanumeric.isValid("abc123"));
        assertFalse(notEmptyAndAlphanumeric.isValid("")); // Fails notEmpty
        assertFalse(notEmptyAndAlphanumeric.isValid("abc-123")); // Fails isAlphanumeric
        assertFalse(notEmptyAndAlphanumeric.isValid(null)); // Fails both
    }

    @Test
    void testValidatorOr() {
        Validators.Validator<String> emptyOrAlphanumeric = Validators.StringValidators.notEmpty.not()
                .or(Validators.StringValidators.isAlphanumeric);
        
        assertTrue(emptyOrAlphanumeric.isValid("")); // Passes empty
        assertTrue(emptyOrAlphanumeric.isValid("abc123")); // Passes alphanumeric
        assertFalse(emptyOrAlphanumeric.isValid("abc-123")); // Fails both
        assertTrue(emptyOrAlphanumeric.isValid(null)); // null is considered "empty"
    }

    @Test
    void testValidatorNot() {
        Validators.Validator<String> notAlphanumeric = Validators.StringValidators.isAlphanumeric.not();
        
        assertTrue(notAlphanumeric.isValid("abc-123"));
        assertTrue(notAlphanumeric.isValid("Hello, World!"));
        assertFalse(notAlphanumeric.isValid("abc123"));
        assertFalse(notAlphanumeric.isValid(""));
        assertTrue(notAlphanumeric.isValid(null)); // null is not alphanumeric
    }

    @Test
    void testComplexValidatorCombination() {
        Validators.Validator<String> notEmptyAndEitherHasDigitOrAllUppercase = 
                Validators.StringValidators.notEmpty
                    .and(Validators.StringValidators.containsDigit
                        .or(s -> s != null && s.equals(s.toUpperCase())));
        
        assertTrue(notEmptyAndEitherHasDigitOrAllUppercase.isValid("abc123")); // Not empty and contains digit
        assertTrue(notEmptyAndEitherHasDigitOrAllUppercase.isValid("ALLCAPS")); // Not empty and all uppercase
        assertTrue(notEmptyAndEitherHasDigitOrAllUppercase.isValid("CAPS123")); // Not empty and both conditions
        assertFalse(notEmptyAndEitherHasDigitOrAllUppercase.isValid("lowercase")); // Not empty but fails both conditions
        assertFalse(notEmptyAndEitherHasDigitOrAllUppercase.isValid("")); // Empty
        assertFalse(notEmptyAndEitherHasDigitOrAllUppercase.isValid(null)); // Null
    }
}