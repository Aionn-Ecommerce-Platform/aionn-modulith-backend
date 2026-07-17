package com.aionn.inventory.adapter.rest.dto.warehouse.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReasonRequest(@NotBlank @Size(max = 500) String reason) {
}

