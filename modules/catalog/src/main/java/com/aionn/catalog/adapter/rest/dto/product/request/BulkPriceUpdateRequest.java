package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BulkPriceUpdateRequest(
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotBlank String productId,
            @NotBlank String skuId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal newPrice,
            @NotBlank String currency) {
    }
}
