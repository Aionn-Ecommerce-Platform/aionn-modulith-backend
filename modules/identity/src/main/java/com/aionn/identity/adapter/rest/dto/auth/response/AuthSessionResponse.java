package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.LocalDateTime;

public record AuthSessionResponse(
                String sessionId,
                String userId,
                String status,
                String ipAddress,
                String userAgent,
                LocalDateTime createdAt,
                LocalDateTime lastActiveAt,
                LocalDateTime expiresAt) {
}
