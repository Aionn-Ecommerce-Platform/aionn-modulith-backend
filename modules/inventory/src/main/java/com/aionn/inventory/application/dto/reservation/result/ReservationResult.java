package com.aionn.inventory.application.dto.reservation.result;

import java.time.Instant;

public record ReservationResult(
        String reservationId,
        String skuId,
        String warehouseId,
        String orderId,
        int qty,
        String status,
        Instant reservedAt,
        Instant expiresAt,
        Instant decidedAt) {
}

