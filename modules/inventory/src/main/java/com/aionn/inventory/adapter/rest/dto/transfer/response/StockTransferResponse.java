package com.aionn.inventory.adapter.rest.dto.transfer.response;

import java.time.Instant;

public record StockTransferResponse(
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
