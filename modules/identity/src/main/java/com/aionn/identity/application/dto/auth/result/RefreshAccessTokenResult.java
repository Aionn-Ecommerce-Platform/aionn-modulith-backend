package com.aionn.identity.application.dto.auth.result;

import java.time.LocalDateTime;

public record RefreshAccessTokenResult(
        String userId,
        String sessionId,
        String accessToken,
        String refreshToken,
        LocalDateTime expiresAt,
        LocalDateTime sessionExpiresAt) {
    @Override
    public String toString() {
        return "RefreshAccessTokenResult[userId=%s, sessionId=%s, accessToken=***, refreshToken=***, expiresAt=%s, sessionExpiresAt=%s]"
                .formatted(userId, sessionId, expiresAt, sessionExpiresAt);
    }
}
