package com.aionn.payment.application.dto.payment.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmPaymentCommand(String paymentId, String transactionNo) implements Command {
}
