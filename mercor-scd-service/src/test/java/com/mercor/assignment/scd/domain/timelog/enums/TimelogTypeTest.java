package com.mercor.assignment.scd.domain.timelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimelogTypeTest {

    @Test
    void fromValue_shouldReturnCorrectEnum() {
        assertEquals(TimelogType.CAPTURED, TimelogType.fromValue("captured"));
        assertEquals(TimelogType.ADJUSTED, TimelogType.fromValue("adjusted"));
        assertNull(TimelogType.fromValue("invalid-type"));
    }

    @Test
    void getValue_shouldReturnCorrectStringValue() {
        assertEquals("captured", TimelogType.CAPTURED.getValue());
        assertEquals("adjusted", TimelogType.ADJUSTED.getValue());
    }

    @Test
    void canTransitionTo_shouldReturnTrueForValidTransitions() {
        // CAPTURED can transition to ADJUSTED
        assertTrue(TimelogType.CAPTURED.canTransitionTo(TimelogType.ADJUSTED));
        
        // CAPTURED cannot transition to itself
        assertFalse(TimelogType.CAPTURED.canTransitionTo(TimelogType.CAPTURED));
        
        // ADJUSTED is a terminal state and cannot transition to anything
        assertFalse(TimelogType.ADJUSTED.canTransitionTo(TimelogType.CAPTURED));
        assertFalse(TimelogType.ADJUSTED.canTransitionTo(TimelogType.ADJUSTED));
    }

    @Test
    void validateTransition_shouldNotThrowForValidTransitions() {
        // Should not throw exceptions
        assertDoesNotThrow(() -> TimelogType.CAPTURED.validateTransition(TimelogType.ADJUSTED));
    }

    @Test
    void validateTransition_shouldThrowForInvalidTransitions() {
        // Should throw exceptions
        assertThrows(IllegalStateException.class, 
            () -> TimelogType.CAPTURED.validateTransition(TimelogType.CAPTURED));
        assertThrows(IllegalStateException.class, 
            () -> TimelogType.ADJUSTED.validateTransition(TimelogType.CAPTURED));
        assertThrows(IllegalStateException.class, 
            () -> TimelogType.ADJUSTED.validateTransition(TimelogType.ADJUSTED));
    }

    @Test
    void validateTransition_shouldIncludeTypeNamesInErrorMessage() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> TimelogType.ADJUSTED.validateTransition(TimelogType.CAPTURED));
        
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("adjusted"));
        assertTrue(errorMessage.contains("captured"));
    }
    
    @Test
    void transitionFlow_shouldFollowBusinessRules() {
        // Test the entire valid flow of transitions
        TimelogType type = TimelogType.CAPTURED;
        
        // Can transition to ADJUSTED
        assertTrue(type.canTransitionTo(TimelogType.ADJUSTED));
        type = TimelogType.ADJUSTED;
        
        // ADJUSTED is terminal state
        assertEquals(0, type.getAllowedTransitions().size());
    }
}