package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.port.in.method.CompleteStripeSetupIntentInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompleteStripeSetupIntentUseCase implements CompleteStripeSetupIntentInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public PaymentMethodResult execute(String userId, String setupIntentId) {
        return paymentMethodService.completeStripeSetupIntent(userId, setupIntentId);
    }
}
