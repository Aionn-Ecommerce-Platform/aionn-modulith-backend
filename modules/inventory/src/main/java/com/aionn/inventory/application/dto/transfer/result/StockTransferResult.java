package com.aionn.inventory.application.dto.transfer.result;

import java.time.Instant;

public record StockTransferResult(
        String transferId,
        String merchantId,
        String fromWarehouseId,
        String toWarehouseId,
        String skuId,
        int qty,
        String status,
        Instant initiatedAt,
        Instant completedAt,
        Instant cancelledAt) {
}

