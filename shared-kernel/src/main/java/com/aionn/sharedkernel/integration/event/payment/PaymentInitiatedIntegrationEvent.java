package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a payment has been initiated (authorisation in progress or
 * pending settlement).
 */
public record PaymentInitiatedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        String gateway,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentInitiatedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
