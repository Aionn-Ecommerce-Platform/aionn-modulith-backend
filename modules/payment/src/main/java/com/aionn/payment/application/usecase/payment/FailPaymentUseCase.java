package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailPaymentUseCase implements FailPaymentInputPort {

    private final PaymentService paymentService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    @Transactional
    public PaymentResult execute(FailPaymentCommand command) {
        return paymentResultMapper.toResult(paymentService.fail(command));
    }
}
