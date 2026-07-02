package com.aionn.identity.application.dto.security.command;

public record CompletePasswordResetCommand(
                String token,
                String newPassword,
                String clientIp) {
}



