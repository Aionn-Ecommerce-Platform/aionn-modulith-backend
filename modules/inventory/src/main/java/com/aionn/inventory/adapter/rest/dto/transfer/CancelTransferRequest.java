package com.aionn.inventory.adapter.rest.dto.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTransferRequest(@NotBlank @Size(max = 500) String reason) {
}
