package com.aionn.catalog.application.dto.category.result;

import java.time.Instant;

public record CategoryResult(
        String categoryId,
        String parentId,
        String name,
        String slug,
        String iconUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}

