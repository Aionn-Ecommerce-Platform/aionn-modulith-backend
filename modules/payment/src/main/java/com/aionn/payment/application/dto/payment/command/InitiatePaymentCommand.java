package com.aionn.payment.application.dto.payment.command;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.sharedkernel.application.command.Command;

public record InitiatePaymentCommand(
        String orderId,
        String userId,
        String paymentMethodId,
        PaymentGatewayKind gateway,
        String idempotencyKey) implements Command {
}
