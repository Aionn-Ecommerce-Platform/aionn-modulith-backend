package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.Instant;

public record AuthSessionResponse(
                String sessionId,
                String userId,
                String status,
                String ipAddress,
                String userAgent,
                Instant createdAt,
                Instant lastActiveAt,
                Instant expiresAt) {
}
