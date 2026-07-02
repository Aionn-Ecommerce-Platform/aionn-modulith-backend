package com.aionn.identity.application.dto.auth.command;

public record LogoutCommand(
        String userId,
        String sessionId,
        String accessTokenJti) {
}
