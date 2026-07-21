package com.aionn.payment.application.dto.method.command;

import com.aionn.sharedkernel.application.command.Command;

public record VerifyMethodCommand(String userId, String methodId) implements Command {
}
