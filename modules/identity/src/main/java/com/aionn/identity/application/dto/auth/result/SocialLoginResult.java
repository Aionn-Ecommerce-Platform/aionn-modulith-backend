package com.aionn.identity.application.dto.auth.result;

import java.time.Instant;

public record SocialLoginResult(
        String userId,
        String sessionId,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        Instant sessionExpiresAt,
        boolean newUser) {
    @Override
    public String toString() {
        return "SocialLoginResult[userId=%s, sessionId=%s, accessToken=***, refreshToken=***, expiresAt=%s, sessionExpiresAt=%s, newUser=%s]"
                .formatted(userId, sessionId, expiresAt, sessionExpiresAt, newUser);
    }
}
