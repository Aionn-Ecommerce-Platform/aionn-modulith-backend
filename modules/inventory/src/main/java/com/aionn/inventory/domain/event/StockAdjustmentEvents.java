package com.aionn.inventory.domain.event;

import com.aionn.inventory.domain.valueobject.AdjustmentType;

import java.time.Instant;

public final class StockAdjustmentEvents {

    private StockAdjustmentEvents() {
    }

    public record ManualAdjustmentRecorded(
            String adjId,
            String skuId,
            String warehouseId,
            int qty,
            AdjustmentType type,
            String reason,
            Instant occurredAt) implements InventoryEvent {
    }

    public record OutboundRecorded(
            String adjId,
            String skuId,
            String warehouseId,
            int qty,
            String orderId,
            Instant occurredAt) implements InventoryEvent {
    }
}

