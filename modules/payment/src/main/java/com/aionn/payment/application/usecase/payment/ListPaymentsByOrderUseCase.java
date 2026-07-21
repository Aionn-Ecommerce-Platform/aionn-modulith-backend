package com.aionn.payment.application.usecase.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ListPaymentsByOrderInputPort;
import com.aionn.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaymentsByOrderUseCase implements ListPaymentsByOrderInputPort {

    private final PaymentService paymentService;

    @Override
    public List<PaymentResult> execute(String orderId) {
        return paymentService.listByOrderId(orderId);
    }
}
