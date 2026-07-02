package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestPasswordResetCommand(
                String identity,
                String clientIp) implements Command {
}



