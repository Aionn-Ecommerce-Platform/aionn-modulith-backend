package com.aionn.sharedkernel.integration.port.payment;

import java.math.BigDecimal;

public interface PaymentInitiatePort {

    InitResult initPayment(
            String orderId,
            String userId,
            String paymentMethodId,
            BigDecimal amount,
            String currency,
            String gatewayKind,
            String idempotencyKey);

    void refund(String paymentId, BigDecimal amount, String currency, String reason);

    record InitResult(String paymentId, String redirectUrl, boolean captured) {
    }
}
