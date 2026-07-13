package com.aionn.inventory.domain.event;

import java.time.Instant;

public final class WarehouseEvents {

    private WarehouseEvents() {
    }

    public record WarehouseCreated(
            String warehouseId,
            String merchantId,
            String address,
            String status,
            Instant occurredAt) implements InventoryEvent {
    }

    public record WarehouseStatusChanged(
            String warehouseId,
            String newStatus,
            Instant occurredAt) implements InventoryEvent {
    }

    public record WarehousePriorityAdjusted(
            String warehouseId,
            String merchantId,
            int priorityLevel,
            Instant updatedAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record WarehouseSuspended(
            String warehouseId,
            String adminId,
            String reason,
            Instant suspendedAt,
            Instant occurredAt) implements InventoryEvent {
    }
}

