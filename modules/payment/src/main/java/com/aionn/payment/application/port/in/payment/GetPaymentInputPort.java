package com.aionn.payment.application.port.in.payment;

import com.aionn.payment.application.dto.payment.result.PaymentResult;

public interface GetPaymentInputPort {
    PaymentResult execute(String paymentId);
}
