package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.LocalDateTime;

public record AuthTokenResponse(
                String userId,
                String sessionId,
                String refreshToken,
                String accessToken,
                LocalDateTime expiresAt,
                LocalDateTime sessionExpiresAt) {
}
