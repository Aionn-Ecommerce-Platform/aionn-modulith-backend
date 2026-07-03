package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestPhoneChangeOtpCommand(
        String userId,
        String newPhone) implements Command {
}
