package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.Instant;

public record AuthTokenResponse(
                String userId,
                String sessionId,
                String refreshToken,
                String accessToken,
                Instant expiresAt,
                Instant sessionExpiresAt) {
}
