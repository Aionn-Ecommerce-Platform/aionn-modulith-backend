package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;

import java.math.BigDecimal;

/**
 * One impl per provider (Stripe, VNPay), routed by {@link PaymentGatewayKind}.
 */
public interface PaymentProviderClient {

        PaymentGatewayKind kind();

        /**
         * Async gateways return {@code authUrl} populated and {@code captured=false}.
         */
        Authorization authorize(AuthorizationRequest request);

        Refund refund(RefundRequest request);

        String generateInvoice(String paymentId, String orderId, BigDecimal amount, String currency);

        WebhookEvent verifyAndParse(String rawBody, String signatureHeader);

        record AuthorizationRequest(
                        String paymentId,
                        String orderId,
                        String userId,
                        String merchantId,
                        String paymentMethodToken,
                        BigDecimal amount,
                        String currency,
                        String idempotencyKey,
                        String returnUrl) {
        }

        record Authorization(
                        boolean captured,
                        String transactionNo,
                        String authUrl,
                        String declineCode,
                        String declineReason) {
        }

        record RefundRequest(
                        String paymentId,
                        String transactionNo,
                        BigDecimal amount,
                        String currency,
                        String reason) {
        }

        record Refund(boolean accepted, String refundTransactionNo, String declineReason) {
        }

        record WebhookEvent(
                        String type,
                        String paymentId,
                        String transactionNo,
                        BigDecimal amount,
                        String currency,
                        boolean success,
                        String errorCode,
                        String errorReason) {
        }
}
