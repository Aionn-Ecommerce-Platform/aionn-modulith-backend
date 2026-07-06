package com.aionn.catalog.adapter.rest.dto.product;

import jakarta.validation.constraints.NotBlank;

public record RemoveVariantRequest(@NotBlank String skuId) {
}
