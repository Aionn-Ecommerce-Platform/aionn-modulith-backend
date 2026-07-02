package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record InitiateMfaSetupCommand(
        String userId,
        String password,
        String clientIp) implements Command {
    @Override
    public String toString() {
        return "InitiateMfaSetupCommand[userId=%s, password=***, clientIp=%s]".formatted(userId, clientIp);
    }
}
