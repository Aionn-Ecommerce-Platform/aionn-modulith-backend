package com.aionn.payment.application.dto.method.result;

public record StripeSetupIntentResult(
        String setupIntentId,
        String clientSecret) {
}
