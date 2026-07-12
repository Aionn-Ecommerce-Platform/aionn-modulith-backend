package com.aionn.identity.application.dto.auth.view;

import java.time.Instant;

public record SocialLinkView(
        String socialLinkId,
        String userId,
        String provider,
        String providerUserId,
        Instant linkedAt) {
}
