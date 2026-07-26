package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.payment.InitiatePaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitiatePaymentUseCase implements InitiatePaymentInputPort {

    private final PaymentService paymentService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    public PaymentResult execute(InitiatePaymentCommand command) {
        var initiation = paymentService.initiate(command);
        var result = paymentResultMapper.toResult(initiation.payment());
        if (initiation.redirectUrl() != null) {
            return result.withRedirectUrl(initiation.redirectUrl());
        }
        return result;
    }
}
