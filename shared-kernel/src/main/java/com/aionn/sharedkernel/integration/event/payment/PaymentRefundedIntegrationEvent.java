package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRefundedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        String refundTransactionNo,
        BigDecimal amount,
        String currency,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentRefundedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
