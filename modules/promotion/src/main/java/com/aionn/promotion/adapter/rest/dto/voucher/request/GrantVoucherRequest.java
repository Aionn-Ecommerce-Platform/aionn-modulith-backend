package com.aionn.promotion.adapter.rest.dto.voucher.request;

import jakarta.validation.constraints.NotBlank;

public record GrantVoucherRequest(
        @NotBlank(message = "userId is required")
        String userId,
        @NotBlank(message = "voucherCode is required")
        String voucherCode,
        String reason,
        /**
         * Optional per-delivery dedup key. When present the grant is idempotent on
         * this key, so the same (user, voucher, key) can only be granted once while
         * other keys still mint their own voucher.
         */
        String entitlementKey
) {
}

