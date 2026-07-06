package com.aionn.catalog.domain.event;

import java.time.Instant;

public final class BrandEvents {

    private BrandEvents() {
    }

    public record BrandCreated(
            String brandId,
            String name,
            String logoUrl,
            String description,
            Instant occurredAt) implements CatalogEvent {
    }

    public record BrandUpdated(
            String brandId,
            String name,
            String logoUrl,
            String description,
            Instant occurredAt) implements CatalogEvent {
    }

    public record BrandDeleted(
            String brandId,
            String reason,
            Instant deletedAt,
            Instant occurredAt) implements CatalogEvent {
    }
}
