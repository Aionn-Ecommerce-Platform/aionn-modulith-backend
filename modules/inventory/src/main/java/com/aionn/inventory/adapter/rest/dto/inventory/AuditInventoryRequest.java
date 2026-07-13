package com.aionn.inventory.adapter.rest.dto.inventory;

import jakarta.validation.constraints.Min;

public record AuditInventoryRequest(@Min(0) int actualQty) {
}
