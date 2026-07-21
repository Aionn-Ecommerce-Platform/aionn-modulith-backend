package com.aionn.payment.infrastructure.integration.ordering;

import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.service.PaymentService;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.sharedkernel.integration.port.payment.PaymentInitiatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentInitiateAdapter implements PaymentInitiatePort {

    private final PaymentService paymentService;

    @Override
    public InitResult initPayment(String orderId, String userId, String paymentMethodId,
            BigDecimal amount, String currency, String gatewayKind, String idempotencyKey) {
        PaymentGatewayKind kind = gatewayKind == null
                ? PaymentGatewayKind.STRIPE
                : PaymentGatewayKind.valueOf(gatewayKind.toUpperCase());
        PaymentResult result = paymentService.initiate(new InitiatePaymentCommand(
                orderId, userId, paymentMethodId, amount, currency, kind, idempotencyKey));
        boolean captured = "PAID".equalsIgnoreCase(result.status());
        return new InitResult(result.paymentId(), result.redirectUrl(), captured);
    }

    @Override
    public void refund(String paymentId, BigDecimal amount, String currency, String reason, String idempotencyKey) {
        paymentService.refund(new RefundPaymentCommand(paymentId, amount, currency, reason));
    }
}
