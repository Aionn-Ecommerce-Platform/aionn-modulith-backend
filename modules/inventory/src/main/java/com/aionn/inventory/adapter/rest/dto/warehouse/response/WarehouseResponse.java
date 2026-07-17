package com.aionn.inventory.adapter.rest.dto.warehouse.response;

import java.time.Instant;

public record WarehouseResponse(
        String warehouseId,
        String merchantId,
        String address,
        int priorityLevel,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
