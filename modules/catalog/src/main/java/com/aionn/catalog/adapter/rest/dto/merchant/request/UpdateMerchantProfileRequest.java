package com.aionn.catalog.adapter.rest.dto.merchant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMerchantProfileRequest(
        @NotBlank @Size(min = 3, max = 150) String name,
        @Size(max = 2048) String logoUrl,
        @Size(max = 2000) String description,
        @Size(max = 10) String provinceCode) {
}

