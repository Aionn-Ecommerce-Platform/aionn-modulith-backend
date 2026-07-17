package com.aionn.catalog.adapter.rest.dto.category.response;

import java.time.Instant;

public record CategoryResponse(
        String categoryId,
        String parentId,
        String name,
        String slug,
        String iconUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
