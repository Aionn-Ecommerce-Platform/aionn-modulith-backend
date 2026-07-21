package com.aionn.payment.adapter.rest.dto.payout.response;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantBalanceResponse(
        String merchantId,
        String currency,
        BigDecimal pending,
        BigDecimal available,
        Instant updatedAt) {
}
