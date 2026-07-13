package com.aionn.inventory.application.dto.analytics.result;

public record LowStockAlertResult(
        String skuId,
        String warehouseId,
        int physicalQty,
        int availableQty,
        int safetyStockQty) {
}
