package com.aionn.promotion.application.dto.voucher.result;

import java.math.BigDecimal;
import java.time.Instant;
import com.aionn.promotion.domain.valueobject.VoucherScope;

public record VoucherResult(
        String voucherCode,
        String campaignId,
        VoucherScope scope,
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
