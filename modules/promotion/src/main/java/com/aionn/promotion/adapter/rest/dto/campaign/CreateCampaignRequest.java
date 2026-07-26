package com.aionn.promotion.adapter.rest.dto.campaign;

import com.aionn.promotion.domain.valueobject.CampaignType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCampaignRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull CampaignType type,
        @NotNull @DecimalMin("0.0") BigDecimal budget,
        @Size(min = 3, max = 3) String currency,
        @NotNull Instant startDate,
        @NotNull Instant endDate) {
}

