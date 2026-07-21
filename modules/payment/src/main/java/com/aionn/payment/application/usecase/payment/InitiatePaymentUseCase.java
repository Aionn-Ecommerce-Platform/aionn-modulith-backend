package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.InitiatePaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitiatePaymentUseCase implements InitiatePaymentInputPort {

    private final PaymentService paymentService;

    @Override
    public PaymentResult execute(InitiatePaymentCommand command) {
        return paymentService.initiate(command);
    }
}
