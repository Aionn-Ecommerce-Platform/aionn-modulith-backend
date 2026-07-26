package com.aionn.promotion.adapter.rest.dto.voucher.response;

import java.math.BigDecimal;
import java.time.Instant;

public record UserVoucherResponse(
        String userVoucherId,
        String voucherCode,
        String userId,
        String status,
        String reservedOrderId,
        BigDecimal appliedAmount,
        String currency,
        Instant claimedAt,
        Instant reservedAt,
        Instant reservedExpiresAt,
        Instant appliedAt,
        Instant releasedAt,
        Instant updatedAt,
        BigDecimal voucherDiscountAmount,
        String voucherCurrency,
        String voucherScope,
        Instant voucherValidUntil,
        BigDecimal minOrderValue,
        int voucherUsageLimit,
        int voucherUsedCount) {
}
