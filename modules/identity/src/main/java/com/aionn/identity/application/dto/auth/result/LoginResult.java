package com.aionn.identity.application.dto.auth.result;

import java.time.LocalDateTime;

public record LoginResult(
                String userId,
                String sessionId,
                String accessToken,
                String refreshToken,
                LocalDateTime expiresAt,
                LocalDateTime sessionExpiresAt) {
}
