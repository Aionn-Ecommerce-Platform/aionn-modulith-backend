package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProductContentRequest(
        @Size(max = 12) List<@NotBlank @Size(max = 2048) String> images,
        @Size(max = 10000) String description,
        List<@NotBlank String> tags) {
}
