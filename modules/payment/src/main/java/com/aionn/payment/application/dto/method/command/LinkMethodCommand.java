package com.aionn.payment.application.dto.method.command;

import com.aionn.sharedkernel.application.command.Command;

public record LinkMethodCommand(
        String userId,
        String provider,
        String last4Digits,
        String gatewayToken) implements Command {
}
