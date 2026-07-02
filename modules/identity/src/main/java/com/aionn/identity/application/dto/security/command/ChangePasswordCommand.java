package com.aionn.identity.application.dto.security.command;

import com.aionn.sharedkernel.application.command.Command;

public record ChangePasswordCommand(
                String userId,
                String currentPassword,
                String newPassword,
                String clientIp) implements Command {
    @Override
    public String toString() {
        return "ChangePasswordCommand[userId=%s, currentPassword=***, newPassword=***, clientIp=%s]"
                .formatted(userId, clientIp);
    }
}

