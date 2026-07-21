package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;

public interface CompleteStripeSetupIntentInputPort {
    PaymentMethodResult execute(String userId, String setupIntentId);
}
