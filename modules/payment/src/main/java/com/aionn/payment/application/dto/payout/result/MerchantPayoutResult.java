package com.aionn.payment.application.dto.payout.result;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantPayoutResult(
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
