package com.aionn.catalog.adapter.rest.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        String parentId,
        @NotBlank @Size(min = 1, max = 150) String name,
        @Size(max = 150) String slug) {
}

