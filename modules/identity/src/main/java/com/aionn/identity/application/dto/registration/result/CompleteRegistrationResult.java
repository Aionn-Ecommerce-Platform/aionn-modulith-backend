package com.aionn.identity.application.dto.registration.result;

import java.time.LocalDateTime;

public record CompleteRegistrationResult(
        String userId,
        String sessionId,
        String refreshToken,
        String accessToken,
        LocalDateTime expiresAt,
        LocalDateTime sessionExpiresAt) {
}
