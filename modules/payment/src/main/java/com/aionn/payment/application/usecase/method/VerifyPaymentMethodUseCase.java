package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.port.in.method.VerifyPaymentMethodInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifyPaymentMethodUseCase implements VerifyPaymentMethodInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public PaymentMethodResult execute(VerifyMethodCommand command) {
        return paymentMethodService.verify(command);
    }
}
