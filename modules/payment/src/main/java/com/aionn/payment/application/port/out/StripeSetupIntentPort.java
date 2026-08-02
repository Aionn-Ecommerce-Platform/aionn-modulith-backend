package com.aionn.payment.application.port.out;

import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;

public interface StripeSetupIntentPort {

    StripeSetupIntentResult create(String userId);

    CompletedSetupIntent complete(String userId, String setupIntentId);

    record CompletedSetupIntent(String provider, String last4, String paymentMethodId) {}
}
