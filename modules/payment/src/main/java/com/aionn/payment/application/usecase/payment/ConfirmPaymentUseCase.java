package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmPaymentUseCase implements ConfirmPaymentInputPort {

    private final PaymentService paymentService;

    @Override
    public PaymentResult execute(ConfirmPaymentCommand command) {
        return paymentService.confirm(command);
    }
}
