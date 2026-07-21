package com.aionn.payment.adapter.rest.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutResponse(
        String payoutId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String status,
        String bankName,
        String bankAccountNo,
        String bankAccountName,
        String externalRef,
        String note,
        Instant requestedAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason) {
}
