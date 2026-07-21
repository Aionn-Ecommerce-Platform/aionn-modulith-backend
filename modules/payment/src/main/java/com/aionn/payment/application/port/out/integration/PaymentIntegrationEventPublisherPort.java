package com.aionn.payment.application.port.out.integration;

import java.math.BigDecimal;

public interface PaymentIntegrationEventPublisherPort {

    void publishPaymentCaptured(String paymentId, String orderId, String transactionNo,
            BigDecimal amount, String currency);

    void publishPaymentFailed(String paymentId, String orderId, String errorCode, String reason);

    void publishPaymentRefunded(String paymentId, String orderId, String refundTransactionNo,
            BigDecimal amount, String currency, String reason);
}
