package com.aionn.catalog.application.dto.brand.result;

import java.time.Instant;

public record BrandResult(
        String brandId,
        String name,
        String logoUrl,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}

