package com.aionn.identity.application.dto.auth.result;

import java.time.Instant;

public record SocialLinkResult(
        String provider,
        String providerUserId,
        Instant linkedAt) {
}

