package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyTakedownRequest(@NotBlank @Size(max = 2000) String reason) {
}
