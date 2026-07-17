package com.aionn.inventory.adapter.rest.dto.warehouse.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Merchant id is derived from the authenticated user, not the request body,
 * so callers cannot create a warehouse under a foreign merchant.
 */
public record CreateWarehouseRequest(
                @Size(max = 1000) String address,
                @Min(0) int priorityLevel) {
}
