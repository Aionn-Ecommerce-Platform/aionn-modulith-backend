package com.aionn.inventory.adapter.rest.dto.inventory.response;

public record LowStockAlertResponse(
        String skuId,
        String warehouseId,
        int physicalQty,
        int availableQty,
        int safetyStockQty) {
}
