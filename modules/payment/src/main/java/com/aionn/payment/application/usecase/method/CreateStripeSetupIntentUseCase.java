package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import com.aionn.payment.application.port.in.method.CreateStripeSetupIntentInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateStripeSetupIntentUseCase implements CreateStripeSetupIntentInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public StripeSetupIntentResult execute(String userId) {
        return paymentMethodService.createStripeSetupIntent(userId);
    }
}
