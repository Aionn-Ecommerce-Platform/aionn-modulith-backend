package com.aionn.inventory.adapter.rest.dto.inventory.response;

import java.time.Instant;
import java.time.LocalDate;

public record InventoryItemResponse(
        String skuId,
        String warehouseId,
        int physicalQty,
        int availableQty,
        int reservedQty,
        int safetyStockQty,
        boolean locked,
        String batchNo,
        LocalDate expiryDate,
        Instant createdAt,
        Instant updatedAt) {
}
