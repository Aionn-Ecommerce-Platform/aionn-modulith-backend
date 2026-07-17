package com.aionn.inventory.adapter.rest.dto.transfer.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InitiateTransferRequest(
        @NotBlank String fromWarehouseId,
        @NotBlank String toWarehouseId,
        @NotBlank String skuId,
        @Positive int qty) {
}
