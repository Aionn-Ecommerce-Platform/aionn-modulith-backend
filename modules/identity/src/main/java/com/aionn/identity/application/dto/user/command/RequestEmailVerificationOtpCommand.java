package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestEmailVerificationOtpCommand(String userId) implements Command {
}
