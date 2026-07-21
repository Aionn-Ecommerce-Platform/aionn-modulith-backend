package com.aionn.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentEvents {

    private PaymentEvents() {
    }

    public record PaymentInitiated(
            String paymentId,
            String orderId,
            BigDecimal amount,
            String currency,
            String gateway,
            String paymentMethodId,
            String idempotencyKey,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentProcessed(
            String paymentId,
            String orderId,
            String transactionNo,
            String gateway,
            BigDecimal amount,
            String currency,
            Instant paidAt,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentFailed(
            String paymentId,
            String orderId,
            String errorCode,
            String reason,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentRefunded(
            String paymentId,
            String orderId,
            String refundId,
            BigDecimal amount,
            String currency,
            String reason,
            Instant occurredAt) implements PaymentEvent {
    }

    public record InvoiceGenerated(
            String paymentId,
            String orderId,
            String invoiceUrl,
            Instant generatedAt,
            Instant occurredAt) implements PaymentEvent {
    }
}

