package com.aionn.payment.domain.event;

import java.time.Instant;

public final class PaymentMethodEvents {

    private PaymentMethodEvents() {
    }

    public record PaymentMethodLinked(
            String methodId,
            String userId,
            String provider,
            String last4Digits,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentMethodVerified(
            String methodId,
            String userId,
            Instant verifiedAt,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentMethodRemoved(
            String methodId,
            String userId,
            Instant occurredAt) implements PaymentEvent {
    }
}

