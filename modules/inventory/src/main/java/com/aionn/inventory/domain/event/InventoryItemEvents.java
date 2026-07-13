package com.aionn.inventory.domain.event;

import java.time.Instant;
import java.time.LocalDate;

public final class InventoryItemEvents {

    private InventoryItemEvents() {
    }

    public record StockInitialized(
            String skuId,
            String warehouseId,
            int initialQty,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockAdjusted(
            String skuId,
            String warehouseId,
            int changeQty,
            String reason,
            Instant occurredAt) implements InventoryEvent {
    }

    public record SafetyStockConfigured(
            String skuId,
            String warehouseId,
            int safetyStockQty,
            Instant configuredAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record SafetyStockBreached(
            String skuId,
            String warehouseId,
            int availableQty,
            int safetyStockQty,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockEmergencyLocked(
            String skuId,
            String warehouseId,
            String adminId,
            String reason,
            Instant lockedAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockEmergencyUnlocked(
            String skuId,
            String warehouseId,
            String adminId,
            Instant unlockedAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record BatchAndExpiryTracked(
            String skuId,
            String warehouseId,
            String batchNo,
            LocalDate expiryDate,
            Instant occurredAt) implements InventoryEvent {
    }

    public record InventoryAudited(
            String auditId,
            String skuId,
            String warehouseId,
            int expectedQty,
            int actualQty,
            Instant auditedAt,
            Instant occurredAt) implements InventoryEvent {
    }
}

