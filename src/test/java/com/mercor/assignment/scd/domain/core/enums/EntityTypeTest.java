package com.mercor.assignment.scd.domain.core.enums;

import static org.junit.jupiter.api.Assertions.*;

import com.mercor.assignment.scd.domain.core.constants.ServiceName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

class EntityTypeTest {

    @Test
    void testEnumValues() {
        // Verify the enum has the expected number of values
        assertEquals(3, EntityType.values().length);
        
        // Verify the enum contains all expected values
        assertTrue(Arrays.asList(EntityType.values()).contains(EntityType.JOBS));
        assertTrue(Arrays.asList(EntityType.values()).contains(EntityType.TIMELOG));
        assertTrue(Arrays.asList(EntityType.values()).contains(EntityType.PAYMENT_LINE_ITEMS));
    }

    @Test
    void testJobsEnum() {
        // Verify JOBS enum properties
        assertEquals("jobs", EntityType.JOBS.getValue());
        assertEquals("job", EntityType.JOBS.getPrefix());
        assertEquals(ServiceName.JOB_SERVICE, EntityType.JOBS.getServiceName());
    }

    @Test
    void testTimelogEnum() {
        // Verify TIMELOG enum properties
        assertEquals("timelog", EntityType.TIMELOG.getValue());
        assertEquals("tl", EntityType.TIMELOG.getPrefix());
        assertEquals(ServiceName.TIMELOG_SERVICE, EntityType.TIMELOG.getServiceName());
    }

    @Test
    void testPaymentLineItemsEnum() {
        // Verify PAYMENT_LINE_ITEMS enum properties
        assertEquals("payment_line_items", EntityType.PAYMENT_LINE_ITEMS.getValue());
        assertEquals("pli", EntityType.PAYMENT_LINE_ITEMS.getPrefix());
        assertEquals(ServiceName.PAYMENT_LINE_ITEMS_SERVICE, EntityType.PAYMENT_LINE_ITEMS.getServiceName());
    }

    @ParameterizedTest
    @MethodSource("provideEntityTypeTestData")
    void testFromValue_ValidValues(String input, EntityType expected) {
        // Verify fromValue returns correct enum for valid inputs
        assertEquals(expected, EntityType.fromValue(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid", "", "job"})
    void testFromValue_InvalidValues(String input) {
        // Verify fromValue throws expected exception for invalid inputs
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> EntityType.fromValue(input)
        );
        assertEquals("Unknown entity type: " + input, exception.getMessage());
    }

    @Test
    void testFromValue_CaseInsensitive() {
        // Verify fromValue is case insensitive
        assertEquals(EntityType.JOBS, EntityType.fromValue("JOBS"));
        assertEquals(EntityType.TIMELOG, EntityType.fromValue("TIMELOG"));
        assertEquals(EntityType.PAYMENT_LINE_ITEMS, EntityType.fromValue("PAYMENT_LINE_ITEMS"));
    }

    /**
     * Provides test data for parameterized tests
     */
    private static Stream<Arguments> provideEntityTypeTestData() {
        return Stream.of(
            Arguments.of("jobs", EntityType.JOBS),
            Arguments.of("timelog", EntityType.TIMELOG),
            Arguments.of("payment_line_items", EntityType.PAYMENT_LINE_ITEMS)
        );
    }
}