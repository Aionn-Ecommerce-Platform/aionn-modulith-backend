package com.aionn.inventory.adapter.rest.dto.inventory.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyLockRequest(@NotBlank @Size(max = 500) String reason) {
}

