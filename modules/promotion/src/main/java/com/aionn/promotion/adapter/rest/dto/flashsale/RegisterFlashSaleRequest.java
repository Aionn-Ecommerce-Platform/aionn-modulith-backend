package com.aionn.promotion.adapter.rest.dto.flashsale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegisterFlashSaleRequest(
        @NotBlank @Size(max = 50) String campaignId,
        @NotBlank @Size(max = 50) String productId,
        @NotBlank @Size(max = 50) String skuId,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        @Size(min = 3, max = 3) String currency,
        @Positive int saleStock) {
}
