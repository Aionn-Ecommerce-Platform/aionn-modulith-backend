package com.aionn.catalog.adapter.rest.dto.brand.response;

import java.time.Instant;

public record BrandResponse(
        String brandId,
        String name,
        String logoUrl,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
