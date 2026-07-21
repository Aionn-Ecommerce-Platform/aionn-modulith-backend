package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FailPaymentUseCase implements FailPaymentInputPort {

    private final PaymentService paymentService;

    @Override
    public PaymentResult execute(FailPaymentCommand command) {
        return paymentService.fail(command);
    }
}
