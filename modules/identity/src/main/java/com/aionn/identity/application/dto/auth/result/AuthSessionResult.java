package com.aionn.identity.application.dto.auth.result;

import java.time.LocalDateTime;

public record AuthSessionResult(
        String sessionId,
        String userId,
        String status,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt,
        LocalDateTime expiresAt) {
}

