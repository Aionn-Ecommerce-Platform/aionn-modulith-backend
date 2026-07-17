package com.aionn.inventory.adapter.rest.dto.reservation.response;

import java.time.Instant;

public record ReservationResponse(
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
