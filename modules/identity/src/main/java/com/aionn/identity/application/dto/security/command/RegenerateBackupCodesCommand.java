package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record RegenerateBackupCodesCommand(
        String userId,
        String password,
        String mfaCode,
        String clientIp) implements Command {
    @Override
    public String toString() {
        return "RegenerateBackupCodesCommand[userId=%s, credential=***, mfaCode=***, clientIp=%s]"
                .formatted(userId, clientIp);
    }
}
