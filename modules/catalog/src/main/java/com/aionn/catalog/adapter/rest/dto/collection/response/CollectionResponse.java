package com.aionn.catalog.adapter.rest.dto.collection.response;

import java.time.Instant;

public record CollectionResponse(
        String collectionId,
        String slug,
        String name,
        String description,
        String imageUrl,
        boolean active,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt) {
}
