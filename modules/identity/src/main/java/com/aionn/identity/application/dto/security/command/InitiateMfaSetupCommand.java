package com.aionn.identity.application.dto.security.command;

public record InitiateMfaSetupCommand(
        String userId,
        String password,
        String clientIp) {
}
