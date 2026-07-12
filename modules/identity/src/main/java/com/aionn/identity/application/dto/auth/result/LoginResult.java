package com.aionn.identity.application.dto.auth.result;

import java.time.Instant;

public record LoginResult(
                String userId,
                String sessionId,
                String accessToken,
                String refreshToken,
                Instant expiresAt,
                Instant sessionExpiresAt) {
    @Override
    public String toString() {
        return "LoginResult[userId=%s, sessionId=%s, accessToken=***, refreshToken=***, expiresAt=%s, sessionExpiresAt=%s]"
                .formatted(userId, sessionId, expiresAt, sessionExpiresAt);
    }
}
