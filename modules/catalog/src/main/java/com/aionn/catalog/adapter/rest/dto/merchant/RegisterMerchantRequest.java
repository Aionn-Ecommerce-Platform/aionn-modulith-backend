package com.aionn.catalog.adapter.rest.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMerchantRequest(
        @NotBlank @Size(min = 3, max = 150) String name) {
}

