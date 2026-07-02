package com.aionn.identity.application.dto.security.command;

public record ChangePasswordCommand(
                String userId,
                String currentPassword,
                String newPassword,
                String clientIp) {
}

