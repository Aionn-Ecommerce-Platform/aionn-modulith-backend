package com.aionn.payment.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    @Test
    void testTransitionsFromInitiated() {
        assertTrue(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.PROCESSING));
        assertTrue(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.FAILED));
        assertFalse(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.REFUNDED));
    }

    @Test
    void testTransitionsFromProcessing() {
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED));
        assertFalse(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.INITIATED));
    }

    @Test
    void testTransitionsFromPaid() {
        assertTrue(PaymentStatus.PAID.canTransitionTo(PaymentStatus.REFUNDED));
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.FAILED));
    }

    @Test
    void testTerminalStates() {
        assertTrue(PaymentStatus.FAILED.isTerminal());
        assertTrue(PaymentStatus.REFUNDED.isTerminal());
        assertFalse(PaymentStatus.PAID.isTerminal());
        assertFalse(PaymentStatus.INITIATED.isTerminal());
        assertFalse(PaymentStatus.PROCESSING.isTerminal());

        assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.PAID));
    }
}
