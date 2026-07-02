package com.aionn.identity.application.dto.auth.view;

import java.time.LocalDateTime;

public record AuthSessionView(
        String sessionId,
        String ipAddress,
        String userAgent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt) {
}
