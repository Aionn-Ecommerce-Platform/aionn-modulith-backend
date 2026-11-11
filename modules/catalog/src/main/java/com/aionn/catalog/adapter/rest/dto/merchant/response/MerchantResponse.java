package com.aionn.catalog.adapter.rest.dto.merchant.response;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantResponse(
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
