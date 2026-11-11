package com.aionn.catalog.application.dto.merchant.result;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantResult(
        String merchantId,
        String ownerId,
        String name,
        String logoUrl,
        String description,
        String provinceCode,
        String provinceName,
        String status,
        BigDecimal commissionRate,
        Instant createdAt,
        Instant updatedAt) {
}
