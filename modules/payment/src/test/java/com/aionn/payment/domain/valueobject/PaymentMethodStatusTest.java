package com.aionn.payment.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodStatusTest {

    @Test
    void testTransitions() {
        assertTrue(PaymentMethodStatus.LINKED.canTransitionTo(PaymentMethodStatus.VERIFIED));
        assertTrue(PaymentMethodStatus.LINKED.canTransitionTo(PaymentMethodStatus.REMOVED));
        
        assertTrue(PaymentMethodStatus.VERIFIED.canTransitionTo(PaymentMethodStatus.REMOVED));
        assertFalse(PaymentMethodStatus.VERIFIED.canTransitionTo(PaymentMethodStatus.LINKED));
        
        assertFalse(PaymentMethodStatus.REMOVED.canTransitionTo(PaymentMethodStatus.VERIFIED));
        assertFalse(PaymentMethodStatus.REMOVED.canTransitionTo(PaymentMethodStatus.LINKED));
    }
}
