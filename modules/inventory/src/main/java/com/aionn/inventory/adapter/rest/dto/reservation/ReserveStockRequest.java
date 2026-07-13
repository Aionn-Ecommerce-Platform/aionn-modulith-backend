package com.aionn.inventory.adapter.rest.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReserveStockRequest(
        @NotBlank String skuId,
        @NotBlank String warehouseId,
        @NotBlank String orderId,
        @Positive int qty,
        @Min(1) int ttlSeconds) {
}

