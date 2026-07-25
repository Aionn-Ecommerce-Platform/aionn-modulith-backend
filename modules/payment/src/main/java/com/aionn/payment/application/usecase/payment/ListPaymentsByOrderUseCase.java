package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.payment.ListPaymentsByOrderInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaymentsByOrderUseCase implements ListPaymentsByOrderInputPort {

    private final PaymentService paymentService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResult> execute(String orderId) {
        return paymentService.listByOrderId(orderId).stream()
                .map(paymentResultMapper::toResult)
                .toList();
    }
}
