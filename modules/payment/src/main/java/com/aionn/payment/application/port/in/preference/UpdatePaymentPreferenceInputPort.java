package com.aionn.payment.application.port.in.preference;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;

public interface UpdatePaymentPreferenceInputPort {
    PaymentPreferenceResult execute(String userId, String paymentType, String paymentMethodId);
}
