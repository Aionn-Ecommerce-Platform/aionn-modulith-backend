package com.aionn.promotion.adapter.rest.dto.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReserveVoucherRequest(
                @NotBlank String orderId,
                @NotNull @DecimalMin("0.0") BigDecimal orderValue,
                @Size(min = 3, max = 3) String currency,
                List<String> orderCategoryIds,
                Instant expiresAt) {
}
