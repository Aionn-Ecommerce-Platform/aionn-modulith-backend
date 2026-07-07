package com.aionn.catalog.adapter.rest.dto.attribute;

import jakarta.validation.constraints.NotBlank;

public record ConfigureFilterableRequest(
        @NotBlank String attributeKey,
        boolean filterable) {
}

