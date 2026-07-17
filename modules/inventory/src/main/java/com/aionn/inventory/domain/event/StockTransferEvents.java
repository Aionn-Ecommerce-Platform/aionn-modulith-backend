package com.aionn.inventory.domain.event;

import java.time.Instant;

public final class StockTransferEvents {

    private StockTransferEvents() {
    }

    public record StockTransferInitiated(
            String transferId,
            String merchantId,
            String fromWarehouseId,
            String toWarehouseId,
            String skuId,
            int qty,
            Instant initiatedAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockTransferCompleted(
            String transferId,
            String merchantId,
            String fromWarehouseId,
            String toWarehouseId,
            String skuId,
            int receivedQty,
            Instant completedAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockTransferCancelled(
            String transferId,
            String merchantId,
            String reason,
            Instant cancelledAt,
            Instant occurredAt) implements InventoryEvent {
    }
}

