package com.aionn.identity.application.dto.auth.view;

import java.time.Instant;

public record AuthSessionView(
        String sessionId,
        String ipAddress,
        String userAgent,
        String status,
        Instant createdAt,
        Instant expiresAt) {
}
