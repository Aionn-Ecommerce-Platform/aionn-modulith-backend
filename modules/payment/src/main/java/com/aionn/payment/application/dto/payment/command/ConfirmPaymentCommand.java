package com.aionn.payment.application.dto.payment.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ConfirmPaymentCommand(
        String paymentId,
        String transactionNo,
        BigDecimal amount,
        String currency) implements Command {
}
