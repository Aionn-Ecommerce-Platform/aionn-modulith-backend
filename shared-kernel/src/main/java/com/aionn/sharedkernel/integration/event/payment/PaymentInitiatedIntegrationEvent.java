package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PaymentInitiatedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        String gateway,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentInitiatedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        currency = Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
