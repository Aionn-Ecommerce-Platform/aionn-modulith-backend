package com.aionn.promotion.adapter.rest.dto.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseVoucherRequest(
                @NotBlank String orderId,
                @NotBlank @Size(max = 500) String reason) {
}
