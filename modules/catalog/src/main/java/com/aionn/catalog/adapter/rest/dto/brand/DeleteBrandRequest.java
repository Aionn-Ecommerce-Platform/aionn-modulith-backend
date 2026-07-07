package com.aionn.catalog.adapter.rest.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteBrandRequest(
        @NotBlank @Size(max = 500) String reason) {
}

