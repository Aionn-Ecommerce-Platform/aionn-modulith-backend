package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.RefundPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundPaymentUseCase implements RefundPaymentInputPort {

    private final PaymentService paymentService;

    @Override
    public PaymentResult execute(RefundPaymentCommand command) {
        return paymentService.refund(command);
    }
}
