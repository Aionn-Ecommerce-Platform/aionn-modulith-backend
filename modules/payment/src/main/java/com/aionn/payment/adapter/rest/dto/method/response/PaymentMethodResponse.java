package com.aionn.payment.adapter.rest.dto.method.response;

import java.time.Instant;

public record PaymentMethodResponse(
        String methodId,
        String userId,
        String provider,
        String last4Digits,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant verifiedAt) {
}
