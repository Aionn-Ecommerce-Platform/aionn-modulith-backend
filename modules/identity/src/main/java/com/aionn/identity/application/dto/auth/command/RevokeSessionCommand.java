package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record RevokeSessionCommand(
                String userId,
                String sessionId) implements Command {
}



