package com.aionn.inventory.adapter.rest.dto.warehouse;

import jakarta.validation.constraints.Min;

public record AdjustPriorityRequest(@Min(0) int priorityLevel) {
}

