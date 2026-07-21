package com.aionn.payment.adapter.rest.dto.method.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteStripeSetupIntentRequest(
        @NotBlank @Size(max = 255) String setupIntentId) {
}
