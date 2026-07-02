package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record LogoutCommand(
        String userId,
        String sessionId,
        String accessTokenJti) implements Command {
}
