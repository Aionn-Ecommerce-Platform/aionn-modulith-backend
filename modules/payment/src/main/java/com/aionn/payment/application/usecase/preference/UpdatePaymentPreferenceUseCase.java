package com.aionn.payment.application.usecase.preference;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import com.aionn.payment.application.port.in.preference.UpdatePaymentPreferenceInputPort;
import com.aionn.payment.application.service.PaymentPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePaymentPreferenceUseCase implements UpdatePaymentPreferenceInputPort {

    private final PaymentPreferenceService paymentPreferenceService;

    @Override
    @Transactional
    public PaymentPreferenceResult execute(String userId, String paymentType, String paymentMethodId) {
        return paymentPreferenceService.update(userId, paymentType, paymentMethodId);
    }
}
