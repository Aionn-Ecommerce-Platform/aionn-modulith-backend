package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestEmailChangeOtpCommand(
        String userId,
        String newEmail) implements Command {
}
