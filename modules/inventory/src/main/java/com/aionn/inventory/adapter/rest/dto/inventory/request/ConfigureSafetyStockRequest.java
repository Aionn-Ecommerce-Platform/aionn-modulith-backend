package com.aionn.inventory.adapter.rest.dto.inventory.request;

import jakarta.validation.constraints.Min;

public record ConfigureSafetyStockRequest(@Min(0) int safetyStockQty) {
}
