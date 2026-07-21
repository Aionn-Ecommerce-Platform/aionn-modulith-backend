package com.aionn.payment.application.port.in.preference;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;

public interface GetPaymentPreferenceInputPort {
    PaymentPreferenceResult execute(String userId);
}
