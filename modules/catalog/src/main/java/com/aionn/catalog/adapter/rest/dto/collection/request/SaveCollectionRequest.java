package com.aionn.catalog.adapter.rest.dto.collection.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCollectionRequest(
        @Size(max = 100) String slug,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 1000) String imageUrl,
        Boolean active,
        Integer displayOrder) {
}
