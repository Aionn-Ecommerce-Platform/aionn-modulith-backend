package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPaymentUseCase implements GetPaymentInputPort {

    private final PaymentService paymentService;

    @Override
    public PaymentResult execute(String paymentId) {
        return paymentService.get(paymentId);
    }
}
