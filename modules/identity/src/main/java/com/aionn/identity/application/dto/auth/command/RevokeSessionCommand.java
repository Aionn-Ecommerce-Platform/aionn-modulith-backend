package com.aionn.identity.application.dto.auth.command;

public record RevokeSessionCommand(
                String userId,
                String sessionId) {
}



