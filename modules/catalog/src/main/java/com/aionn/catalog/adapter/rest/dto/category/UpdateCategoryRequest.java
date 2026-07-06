package com.aionn.catalog.adapter.rest.dto.category;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 1, max = 150) String name,
        @Size(max = 2048) String iconUrl,
        Boolean active) {
}

