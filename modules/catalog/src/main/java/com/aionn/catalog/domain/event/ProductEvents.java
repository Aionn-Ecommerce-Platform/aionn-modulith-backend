package com.aionn.catalog.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ProductEvents {

    private ProductEvents() {
    }

    public record ProductCreated(
            String productId,
            String merchantId,
            String name,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductVariantDefined(
            String productId,
            String skuId,
            Map<String, String> attributeValues,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductMediaUpdated(
            String productId,
            List<String> imageList,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductCategorized(
            String productId,
            List<String> categoryIds,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductBrandAssigned(
            String productId,
            String brandId,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductPublished(
            String productId,
            String adminId,
            Instant publishedAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductSubmittedForReview(
            String productId,
            String ownerId,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductRejected(
            String productId,
            String adminId,
            String reasonCode,
            String feedback,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductDeactivated(
            String productId,
            String merchantId,
            String reason,
            Instant deactivatedAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductRestored(
            String productId,
            Instant restoredAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductCloned(
            String sourceId,
            String targetId,
            String merchantId,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductVariantRemoved(
            String productId,
            String skuId,
            String merchantId,
            Instant removedAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductVariantPriceChanged(
            String productId,
            String skuId,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            String currency,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductMetadataUpdated(
            String productId,
            List<String> tags,
            String aiDescription,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductCollectionAssigned(
            String productId,
            List<String> collectionIds,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductBulkPriceUpdated(
            String merchantId,
            List<String> skuIds,
            String changeType,
            BigDecimal value,
            String currency,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductEmergencyTakedown(
            String productId,
            String adminId,
            String reason,
            Instant takedownAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record ProductAttributesDefined(
            String productId,
            Map<String, String> attributes,
            Instant occurredAt) implements CatalogEvent {
    }
}
