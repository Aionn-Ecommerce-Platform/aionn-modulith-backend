package com.aionn.inventory.adapter.rest.dto.inventory;

import jakarta.validation.constraints.Min;

public record ConfigureSafetyStockRequest(@Min(0) int safetyStockQty) {
}
