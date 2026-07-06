package com.aionn.catalog.application.dto.merchant.result;

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
        Instant createdAt,
        Instant updatedAt) {
}
