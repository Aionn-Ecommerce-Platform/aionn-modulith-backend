package com.aionn.promotion.adapter.rest.dto.voucher.response;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResponse(
        String voucherCode,
        String campaignId,
        String scope,
        String merchantId,
        BigDecimal discountAmount,
        String currency,
        int usageLimit,
        int usedCount,
        int reservedCount,
        Instant validFrom,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt) {
}
