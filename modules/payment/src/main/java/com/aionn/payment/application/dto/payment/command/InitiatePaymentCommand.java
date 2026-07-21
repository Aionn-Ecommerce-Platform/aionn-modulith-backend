package com.aionn.payment.application.dto.payment.command;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record InitiatePaymentCommand(
        String orderId,
        String userId,
        String paymentMethodId,
        BigDecimal amount,
        String currency,
        PaymentGatewayKind gateway,
        String idempotencyKey) implements Command {
}
