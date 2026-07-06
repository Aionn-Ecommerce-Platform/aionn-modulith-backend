package com.aionn.catalog.adapter.rest.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(
        @NotBlank @Size(min = 1, max = 150) String name,
        @Size(max = 2048) String logoUrl,
        @Size(max = 2000) String description) {
}

