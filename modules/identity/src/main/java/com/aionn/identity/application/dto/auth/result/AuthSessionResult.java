package com.aionn.identity.application.dto.auth.result;

import java.time.Instant;

public record AuthSessionResult(
        String sessionId,
        String userId,
        String status,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastActiveAt,
        Instant expiresAt) {
}

