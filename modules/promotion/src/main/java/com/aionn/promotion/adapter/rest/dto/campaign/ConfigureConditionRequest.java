package com.aionn.promotion.adapter.rest.dto.campaign;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public record ConfigureConditionRequest(
        @DecimalMin("0.0") BigDecimal minOrderValue,
        List<String> applicableCategoryIds,
        @Min(1) Integer maxClaimsPerUser,
        @Min(1) Integer maxUsesPerVoucher) {
}

