package com.aionn.identity.application.dto.auth.command;

public record LoginCommand(
                String identity,
                String password,
                String mfaCode,
                String ipAddress,
                String userAgent) {
}
