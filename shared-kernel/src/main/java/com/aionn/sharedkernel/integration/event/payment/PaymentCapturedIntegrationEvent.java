package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCapturedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        String transactionNo,
        BigDecimal amount,
        String currency,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentCapturedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
