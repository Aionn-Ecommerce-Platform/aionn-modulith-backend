package com.aionn.payment.adapter.rest.dto.method.response;

public record StripeSetupIntentResponse(String setupIntentId, String clientSecret) {
}
