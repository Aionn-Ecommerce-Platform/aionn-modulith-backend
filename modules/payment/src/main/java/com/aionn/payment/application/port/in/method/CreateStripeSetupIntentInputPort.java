package com.aionn.payment.application.port.in.method;

import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;

public interface CreateStripeSetupIntentInputPort {
    StripeSetupIntentResult execute(String userId);
}
