package com.aionn.inventory.domain.event;

import java.time.Instant;

public final class StockReservationEvents {

    private StockReservationEvents() {
    }

    public record StockReserved(
            String reservationId,
            String skuId,
            String warehouseId,
            int qty,
            Instant expiresAt,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockReservationFailed(
            String reservationId,
            String skuId,
            String warehouseId,
            int qty,
            String reason,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockCommitted(
            String reservationId,
            String skuId,
            String warehouseId,
            String orderId,
            int qty,
            Instant occurredAt) implements InventoryEvent {
    }

    public record StockReleased(
            String reservationId,
            String skuId,
            String warehouseId,
            String orderId,
            int qty,
            String reason,
            Instant occurredAt) implements InventoryEvent {
    }
}

