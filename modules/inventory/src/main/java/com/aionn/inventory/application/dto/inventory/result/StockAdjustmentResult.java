package com.aionn.inventory.application.dto.inventory.result;

import java.time.Instant;

public record StockAdjustmentResult(
        String adjId,
        String skuId,
        String warehouseId,
        int qty,
        String type,
        String reason,
        String orderId,
        Instant occurredAt) {
}
