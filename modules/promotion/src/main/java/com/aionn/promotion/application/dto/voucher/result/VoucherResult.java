package com.aionn.promotion.application.dto.voucher.result;

import com.aionn.promotion.domain.valueobject.VoucherScope;
import com.aionn.promotion.domain.valueobject.VoucherType;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResult(
        String voucherCode,
        String campaignId,
        VoucherScope scope,
        String merchantId,
        VoucherType type,
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
