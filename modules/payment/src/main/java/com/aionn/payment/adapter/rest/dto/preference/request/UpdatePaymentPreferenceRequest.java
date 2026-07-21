package com.aionn.payment.adapter.rest.dto.preference.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePaymentPreferenceRequest(
        @NotBlank String paymentType,
        String paymentMethodId) {
}
