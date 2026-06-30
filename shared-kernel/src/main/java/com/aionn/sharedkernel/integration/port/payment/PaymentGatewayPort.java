package com.aionn.sharedkernel.integration.port.payment;

import java.math.BigDecimal;

/**
 * Outbound port for authorising and refunding payments.
 *
 * <p>
 * Used synchronously by the Ordering module during checkout. Authorisation
 * must complete before the order can be placed.
 * </p>
 */
public interface PaymentGatewayPort {

    PaymentAuthorization authorize(String orderId, String userId, String paymentMethodId,
            BigDecimal amount, String currency);

    void refund(String paymentId, BigDecimal amount, String currency, String reason);

    record PaymentAuthorization(String paymentId, boolean approved, String declineReason) {
    }
}
