package com.aionn.identity.application.dto.registration.result;

import java.time.Instant;

public record CompleteRegistrationResult(
        String userId,
        String sessionId,
        String refreshToken,
        String accessToken,
        Instant expiresAt,
        Instant sessionExpiresAt) {
    @Override
    public String toString() {
        return "CompleteRegistrationResult[userId=%s, sessionId=%s, refreshToken=***, accessToken=***, expiresAt=%s, sessionExpiresAt=%s]"
                .formatted(userId, sessionId, expiresAt, sessionExpiresAt);
    }
}
