package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record DisableMfaCommand(
                String userId,
                String password,
                String mfaCode,
                String clientIp) implements Command {
    @Override
    public String toString() {
        return "DisableMfaCommand[userId=%s, password=***, mfaCode=***, clientIp=%s]"
                .formatted(userId, clientIp);
    }
}


