package com.aionn.catalog.domain.event;

import java.time.Instant;

public final class MerchantEvents {

    private MerchantEvents() {
    }

    public record MerchantRegistered(
            String merchantId,
            String ownerId,
            String name,
            String status,
            Instant occurredAt) implements CatalogEvent {
    }

    public record MerchantProfileUpdated(
            String merchantId,
            String name,
            String logoUrl,
            String description,
            String provinceCode,
            String provinceName,
            boolean provinceChanged,
            Instant occurredAt) implements CatalogEvent {
    }

    public record MerchantSuspended(
            String merchantId,
            String reason,
            String adminId,
            Instant suspendedAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record MerchantActivated(
            String merchantId,
            String adminId,
            String reason,
            Instant activatedAt,
            Instant occurredAt) implements CatalogEvent {
    }

    public record MerchantClosed(
            String merchantId,
            String reason,
            Instant closedAt,
            Instant occurredAt) implements CatalogEvent {
    }
}
