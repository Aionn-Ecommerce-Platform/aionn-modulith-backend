package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;

/**
 * Outbound port for payment authorize / refund used during order placement and
 * cancellation.
 */
public interface PaymentGateway {

    PaymentAuthorization authorize(String orderId, String userId, String paymentMethodId,
            BigDecimal amount, String currency, String gateway);

    void refund(String paymentId, BigDecimal amount, String currency, String reason);

    record PaymentAuthorization(String paymentId, boolean approved, String declineReason) {
    }
}
