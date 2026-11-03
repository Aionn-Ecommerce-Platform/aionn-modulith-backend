package com.aionn.promotion.adapter.rest.dto.voucher;

import jakarta.validation.constraints.NotBlank;

public record GrantVoucherRequest(
        @NotBlank(message = "userId is required")
        String userId,
        @NotBlank(message = "voucherCode is required")
        String voucherCode,
        String reason
) {
}

