package com.aionn.payment.application.dto.payment.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record RefundPaymentCommand(
        String paymentId,
        BigDecimal amount,
        String currency,
        String reason) implements Command {
}
