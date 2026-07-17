package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;

public record AssignBrandRequest(@NotBlank String brandId) {
}
