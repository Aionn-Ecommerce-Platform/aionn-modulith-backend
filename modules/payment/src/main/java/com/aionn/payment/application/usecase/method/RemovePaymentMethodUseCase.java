package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.port.in.method.RemovePaymentMethodInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemovePaymentMethodUseCase implements RemovePaymentMethodInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public void execute(RemoveMethodCommand command) {
        paymentMethodService.remove(command);
    }
}
