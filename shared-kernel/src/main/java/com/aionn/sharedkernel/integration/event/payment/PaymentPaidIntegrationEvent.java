package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a payment has been successfully captured.
 */
public record PaymentPaidIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        String gateway,
        String transactionNo,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentPaidIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
