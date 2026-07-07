package com.aionn.catalog.domain.event;

import java.time.Instant;

public final class CategoryEvents {

    private CategoryEvents() {
    }

    public record CategoryCreated(
            String categoryId,
            String parentId,
            String name,
            String slug,
            Instant occurredAt) implements CatalogEvent {
    }

    public record CategoryUpdated(
            String categoryId,
            String name,
            String iconUrl,
            boolean active,
            Instant occurredAt) implements CatalogEvent {
    }

    public record CategoryMoved(
            String categoryId,
            String oldParentId,
            String newParentId,
            Instant occurredAt) implements CatalogEvent {
    }

    public record CategoryDeleted(
            String categoryId,
            Instant deletedAt,
            Instant occurredAt) implements CatalogEvent {
    }
}
