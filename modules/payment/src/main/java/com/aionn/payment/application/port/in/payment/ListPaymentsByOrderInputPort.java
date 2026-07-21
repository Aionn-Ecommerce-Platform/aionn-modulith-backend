package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;

import java.util.List;

public interface ListPaymentsByOrderInputPort {
    List<PaymentResult> execute(String orderId);
}
