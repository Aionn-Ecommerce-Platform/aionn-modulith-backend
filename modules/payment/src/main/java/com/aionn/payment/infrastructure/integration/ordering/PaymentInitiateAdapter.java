package com.aionn.payment.infrastructure.integration.ordering;

import com.aionn.payment.application.dto.payment.PaymentInitiation;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.service.PaymentService;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.PaymentStatus;
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
        PaymentInitiation initiation = paymentService.initiate(new InitiatePaymentCommand(
                orderId, userId, paymentMethodId, amount, currency, kind, idempotencyKey));
        Payment payment = initiation.payment();
        boolean captured = payment.getStatus() == PaymentStatus.PAID;
        return new InitResult(payment.getPaymentId(), initiation.redirectUrl(), captured);
    }

    @Override
    public void refund(String paymentId, BigDecimal amount, String currency, String reason, String idempotencyKey) {
        paymentService.refund(new RefundPaymentCommand(paymentId, amount, currency, reason));
    }
}
