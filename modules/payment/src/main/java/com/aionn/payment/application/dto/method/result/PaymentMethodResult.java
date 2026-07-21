package com.aionn.payment.application.dto.method.result;

import java.time.Instant;

public record PaymentMethodResult(
        String methodId,
        String userId,
        String provider,
        String last4Digits,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant verifiedAt) {
}

