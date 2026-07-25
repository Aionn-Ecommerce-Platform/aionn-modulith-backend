package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPaymentUseCase implements GetPaymentInputPort {

    private final PaymentService paymentService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PaymentResult execute(String paymentId) {
        return paymentResultMapper.toResult(paymentService.get(paymentId));
    }
}
