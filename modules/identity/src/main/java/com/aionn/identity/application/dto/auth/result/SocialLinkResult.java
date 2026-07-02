package com.aionn.identity.application.dto.auth.result;

import java.time.LocalDateTime;

public record SocialLinkResult(
        String provider,
        String providerUserId,
        LocalDateTime linkedAt) {
}

