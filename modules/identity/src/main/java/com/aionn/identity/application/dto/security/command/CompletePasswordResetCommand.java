package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record CompletePasswordResetCommand(
                String token,
                String newPassword,
                String clientIp) implements Command {
    @Override
    public String toString() {
        return "CompletePasswordResetCommand[token=***, newCredential=***, clientIp=%s]".formatted(clientIp);
    }
}



