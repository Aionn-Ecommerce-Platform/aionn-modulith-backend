package com.aionn.payment.application.dto.payment.result;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResult(
                String paymentId,
                String orderId,
                String userId,
                String paymentMethodId,
                BigDecimal amount,
                BigDecimal refundedAmount,
                String currency,
                String gateway,
                String status,
                String transactionNo,
                String invoiceUrl,
                String errorCode,
                String errorReason,
                Instant createdAt,
                Instant updatedAt,
                Instant paidAt,
                Instant failedAt,
                String redirectUrl) {

        public PaymentResult(
                        String paymentId,
                        String orderId,
                        String userId,
                        String paymentMethodId,
                        BigDecimal amount,
                        BigDecimal refundedAmount,
                        String currency,
                        String gateway,
                        String status,
                        String transactionNo,
                        String invoiceUrl,
                        String errorCode,
                        String errorReason,
                        Instant createdAt,
                        Instant updatedAt,
                        Instant paidAt,
                        Instant failedAt) {
                this(paymentId, orderId, userId, paymentMethodId, amount, refundedAmount, currency,
                                gateway, status, transactionNo, invoiceUrl, errorCode, errorReason,
                                createdAt, updatedAt, paidAt, failedAt, null);
        }

        public PaymentResult withRedirectUrl(String url) {
                return new PaymentResult(paymentId, orderId, userId, paymentMethodId, amount,
                                refundedAmount, currency, gateway, status, transactionNo, invoiceUrl,
                                errorCode, errorReason, createdAt, updatedAt, paidAt, failedAt, url);
        }
}
