package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.port.in.method.LinkPaymentMethodInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkPaymentMethodUseCase implements LinkPaymentMethodInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public PaymentMethodResult execute(LinkMethodCommand command) {
        return paymentMethodService.link(command);
    }
}
