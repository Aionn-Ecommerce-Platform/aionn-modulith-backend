package com.aionn.identity.application.dto.auth.result;

import java.time.LocalDateTime;

public record SocialLoginResult(
        String userId,
        String sessionId,
        String accessToken,
        String refreshToken,
        LocalDateTime expiresAt,
        LocalDateTime sessionExpiresAt,
        boolean newUser) {
}
