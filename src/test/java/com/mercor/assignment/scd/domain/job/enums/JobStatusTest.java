package com.mercor.assignment.scd.domain.job.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobStatusTest {

    @Test
    void fromValue_shouldReturnCorrectEnum() {
        assertEquals(JobStatus.ACTIVE, JobStatus.fromValue("active"));
        assertEquals(JobStatus.EXTENDED, JobStatus.fromValue("extended"));
        assertEquals(JobStatus.COMPLETED, JobStatus.fromValue("completed"));
        assertNull(JobStatus.fromValue("invalid-status"));
    }

    @Test
    void getValue_shouldReturnCorrectStringValue() {
        assertEquals("active", JobStatus.ACTIVE.getValue());
        assertEquals("extended", JobStatus.EXTENDED.getValue());
        assertEquals("completed", JobStatus.COMPLETED.getValue());
    }

    @Test
    void canTransitionTo_shouldReturnTrueForValidTransitions() {
        // Test ACTIVE transitions
        assertTrue(JobStatus.ACTIVE.canTransitionTo(JobStatus.EXTENDED));
        assertTrue(JobStatus.ACTIVE.canTransitionTo(JobStatus.COMPLETED));
        assertFalse(JobStatus.ACTIVE.canTransitionTo(JobStatus.ACTIVE));

        // Test EXTENDED transitions
        assertTrue(JobStatus.EXTENDED.canTransitionTo(JobStatus.ACTIVE));
        assertTrue(JobStatus.EXTENDED.canTransitionTo(JobStatus.COMPLETED));
        assertFalse(JobStatus.EXTENDED.canTransitionTo(JobStatus.EXTENDED));

        // Test COMPLETED transitions (should have none)
        assertFalse(JobStatus.COMPLETED.canTransitionTo(JobStatus.ACTIVE));
        assertFalse(JobStatus.COMPLETED.canTransitionTo(JobStatus.EXTENDED));
        assertFalse(JobStatus.COMPLETED.canTransitionTo(JobStatus.COMPLETED));
    }

    @Test
    void validateTransition_shouldNotThrowForValidTransitions() {
        // Should not throw exceptions
        assertDoesNotThrow(() -> JobStatus.ACTIVE.validateTransition(JobStatus.EXTENDED));
        assertDoesNotThrow(() -> JobStatus.ACTIVE.validateTransition(JobStatus.COMPLETED));
        assertDoesNotThrow(() -> JobStatus.EXTENDED.validateTransition(JobStatus.ACTIVE));
        assertDoesNotThrow(() -> JobStatus.EXTENDED.validateTransition(JobStatus.COMPLETED));
    }

    @Test
    void validateTransition_shouldThrowForInvalidTransitions() {
        // Should throw exceptions
        assertThrows(IllegalStateException.class, 
            () -> JobStatus.ACTIVE.validateTransition(JobStatus.ACTIVE));
        assertThrows(IllegalStateException.class, 
            () -> JobStatus.EXTENDED.validateTransition(JobStatus.EXTENDED));
        assertThrows(IllegalStateException.class, 
            () -> JobStatus.COMPLETED.validateTransition(JobStatus.ACTIVE));
        assertThrows(IllegalStateException.class, 
            () -> JobStatus.COMPLETED.validateTransition(JobStatus.EXTENDED));
        assertThrows(IllegalStateException.class, 
            () -> JobStatus.COMPLETED.validateTransition(JobStatus.COMPLETED));
    }

    @Test
    void validateTransition_shouldIncludeStatusNamesInErrorMessage() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> JobStatus.COMPLETED.validateTransition(JobStatus.ACTIVE));
        
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("completed"));
        assertTrue(errorMessage.contains("active"));
    }
}