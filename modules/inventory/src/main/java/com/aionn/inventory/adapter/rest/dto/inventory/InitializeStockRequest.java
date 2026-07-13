package com.aionn.inventory.adapter.rest.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InitializeStockRequest(
                @NotBlank String skuId,
                @NotBlank String warehouseId,
                @Min(0) int initialQty) {
}
