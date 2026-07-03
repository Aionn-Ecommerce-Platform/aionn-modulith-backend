package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmPhoneChangeCommand(
        String userId,
        String otpCode) implements Command {
}
