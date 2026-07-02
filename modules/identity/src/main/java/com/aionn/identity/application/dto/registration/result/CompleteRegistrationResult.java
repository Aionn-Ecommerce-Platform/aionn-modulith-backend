package com.aionn.identity.application.dto.registration.result;

import java.time.LocalDateTime;

public record CompleteRegistrationResult(
        String userId,
        String sessionId,
        String refreshToken,
        String accessToken,
        LocalDateTime expiresAt,
        LocalDateTime sessionExpiresAt) {
    @Override
    public String toString() {
        return "CompleteRegistrationResult[userId=%s, sessionId=%s, refreshToken=***, accessToken=***, expiresAt=%s, sessionExpiresAt=%s]"
                .formatted(userId, sessionId, expiresAt, sessionExpiresAt);
    }
}
