package com.aionn.catalog.adapter.rest.dto.merchant.response;

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
        Instant createdAt,
        Instant updatedAt) {
}
