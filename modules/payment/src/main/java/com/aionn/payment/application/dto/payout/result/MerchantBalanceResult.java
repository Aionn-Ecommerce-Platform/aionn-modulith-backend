package com.aionn.payment.application.dto.payout.result;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantBalanceResult(
        String merchantId,
        String currency,
        BigDecimal pending,
        BigDecimal available,
        Instant updatedAt) {
}
