package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.LocalDateTime;

public record SocialAuthResponse(
        String userId,
        String sessionId,
        String accessToken,
        LocalDateTime expiresAt) {
}


