package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record LoginCommand(
                String identity,
                String password,
                String mfaCode,
                String ipAddress,
                String userAgent) implements Command {
    @Override
    public String toString() {
        return "LoginCommand[identity=%s, credential=***, mfaCode=***, ipAddress=%s, userAgent=%s]"
                .formatted(identity, ipAddress, userAgent);
    }
}
