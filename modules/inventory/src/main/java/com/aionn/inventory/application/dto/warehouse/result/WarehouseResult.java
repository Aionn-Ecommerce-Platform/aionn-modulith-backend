package com.aionn.inventory.application.dto.warehouse.result;

import java.time.Instant;

public record WarehouseResult(
        String warehouseId,
        String merchantId,
        String address,
        int priorityLevel,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}

