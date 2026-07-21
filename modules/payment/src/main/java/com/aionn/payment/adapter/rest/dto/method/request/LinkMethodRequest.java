package com.aionn.payment.adapter.rest.dto.method.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkMethodRequest(
        @NotBlank @Size(max = 50) String provider,
        @Size(min = 4, max = 4) String last4Digits,
        @NotBlank @Size(max = 255) String gatewayToken) {
}
