package com.aionn.inventory.application.dto.inventory.result;

import java.time.Instant;
import java.time.LocalDate;

public record InventoryItemResult(
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

