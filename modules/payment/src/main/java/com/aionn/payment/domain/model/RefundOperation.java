package com.aionn.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundOperation(
        String idempotencyKey,
        String paymentId,
        BigDecimal amount,
        String currency,
        String reason,
        Status status,
        String providerRefundId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public enum Status {
        PENDING,
        SUCCEEDED,
        FAILED
    }

    public RefundOperation succeeded(String providerRefundId, Instant now) {
        return new RefundOperation(idempotencyKey, paymentId, amount, currency, reason,
                Status.SUCCEEDED, providerRefundId, null, createdAt, now);
    }

    public RefundOperation failed(String failureReason, Instant now) {
        return new RefundOperation(idempotencyKey, paymentId, amount, currency, reason,
                Status.FAILED, null, failureReason, createdAt, now);
    }
}
