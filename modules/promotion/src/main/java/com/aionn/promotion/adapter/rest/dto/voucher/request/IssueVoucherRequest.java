package com.aionn.promotion.adapter.rest.dto.voucher.request;

import com.aionn.promotion.domain.valueobject.VoucherType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record IssueVoucherRequest(
        @NotBlank @Size(max = 50) String voucherCode,
        @NotNull @DecimalMin("0.0") BigDecimal discountAmount,
        @Size(min = 3, max = 3) String currency,
        @Positive int usageLimit,
        Instant validFrom,
        Instant validUntil,
        /** ORDER_DISCOUNT (default) reduces goods; FREE_SHIPPING waives shipping up to the amount. */
        VoucherType type) {
}

