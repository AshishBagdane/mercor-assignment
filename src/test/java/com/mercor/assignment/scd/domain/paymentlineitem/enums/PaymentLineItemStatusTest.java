package com.mercor.assignment.scd.domain.paymentlineitem.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentLineItemStatusTest {

    @Test
    void fromValue_shouldReturnCorrectEnum() {
        assertEquals(PaymentLineItemStatus.NOT_PAID, PaymentLineItemStatus.fromValue("not-paid"));
        assertEquals(PaymentLineItemStatus.PROCESSING, PaymentLineItemStatus.fromValue("processing"));
        assertEquals(PaymentLineItemStatus.PAID, PaymentLineItemStatus.fromValue("paid"));
        assertNull(PaymentLineItemStatus.fromValue("invalid-status"));
    }

    @Test
    void getValue_shouldReturnCorrectStringValue() {
        assertEquals("not-paid", PaymentLineItemStatus.NOT_PAID.getValue());
        assertEquals("processing", PaymentLineItemStatus.PROCESSING.getValue());
        assertEquals("paid", PaymentLineItemStatus.PAID.getValue());
    }

    @Test
    void canTransitionTo_shouldReturnTrueForValidTransitions() {
        // Test NOT_PAID transitions
        assertTrue(PaymentLineItemStatus.NOT_PAID.canTransitionTo(PaymentLineItemStatus.PROCESSING));
        assertFalse(PaymentLineItemStatus.NOT_PAID.canTransitionTo(PaymentLineItemStatus.PAID));
        assertFalse(PaymentLineItemStatus.NOT_PAID.canTransitionTo(PaymentLineItemStatus.NOT_PAID));

        // Test PROCESSING transitions
        assertTrue(PaymentLineItemStatus.PROCESSING.canTransitionTo(PaymentLineItemStatus.NOT_PAID));
        assertTrue(PaymentLineItemStatus.PROCESSING.canTransitionTo(PaymentLineItemStatus.PAID));
        assertFalse(PaymentLineItemStatus.PROCESSING.canTransitionTo(PaymentLineItemStatus.PROCESSING));

        // Test PAID transitions (should have none)
        assertFalse(PaymentLineItemStatus.PAID.canTransitionTo(PaymentLineItemStatus.NOT_PAID));
        assertFalse(PaymentLineItemStatus.PAID.canTransitionTo(PaymentLineItemStatus.PROCESSING));
        assertFalse(PaymentLineItemStatus.PAID.canTransitionTo(PaymentLineItemStatus.PAID));
    }

    @Test
    void validateTransition_shouldNotThrowForValidTransitions() {
        // Should not throw exceptions
        assertDoesNotThrow(() -> PaymentLineItemStatus.NOT_PAID.validateTransition(PaymentLineItemStatus.PROCESSING));
        assertDoesNotThrow(() -> PaymentLineItemStatus.PROCESSING.validateTransition(PaymentLineItemStatus.NOT_PAID));
        assertDoesNotThrow(() -> PaymentLineItemStatus.PROCESSING.validateTransition(PaymentLineItemStatus.PAID));
    }

    @Test
    void validateTransition_shouldThrowForInvalidTransitions() {
        // Should throw exceptions
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.NOT_PAID.validateTransition(PaymentLineItemStatus.PAID));
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.NOT_PAID.validateTransition(PaymentLineItemStatus.NOT_PAID));
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.PROCESSING.validateTransition(PaymentLineItemStatus.PROCESSING));
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.PAID.validateTransition(PaymentLineItemStatus.NOT_PAID));
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.PAID.validateTransition(PaymentLineItemStatus.PROCESSING));
        assertThrows(IllegalStateException.class, 
            () -> PaymentLineItemStatus.PAID.validateTransition(PaymentLineItemStatus.PAID));
    }

    @Test
    void validateTransition_shouldIncludeStatusNamesInErrorMessage() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> PaymentLineItemStatus.PAID.validateTransition(PaymentLineItemStatus.NOT_PAID));
        
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("paid"));
        assertTrue(errorMessage.contains("not-paid"));
    }

    @Test
    void transitionFlow_completePaymentProcess() {
        // Test a complete payment flow with status transitions
        PaymentLineItemStatus status = PaymentLineItemStatus.NOT_PAID;
        
        // Can transition to PROCESSING
        assertTrue(status.canTransitionTo(PaymentLineItemStatus.PROCESSING));
        status = PaymentLineItemStatus.PROCESSING;
        
        // Can transition to PAID
        assertTrue(status.canTransitionTo(PaymentLineItemStatus.PAID));
        status = PaymentLineItemStatus.PAID;
        
        // PAID is terminal state
        assertEquals(0, status.getAllowedTransitions().size());
    }
}