package com.aionn.payment.application.usecase.preference;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import com.aionn.payment.application.port.in.preference.GetPaymentPreferenceInputPort;
import com.aionn.payment.application.service.PaymentPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPaymentPreferenceUseCase implements GetPaymentPreferenceInputPort {

    private final PaymentPreferenceService paymentPreferenceService;

    @Override
    public PaymentPreferenceResult execute(String userId) {
        return paymentPreferenceService.get(userId);
    }
}
