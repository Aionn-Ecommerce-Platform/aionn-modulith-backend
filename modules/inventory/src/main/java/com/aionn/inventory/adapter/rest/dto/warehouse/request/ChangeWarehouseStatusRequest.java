package com.aionn.inventory.adapter.rest.dto.warehouse.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeWarehouseStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) {
}

