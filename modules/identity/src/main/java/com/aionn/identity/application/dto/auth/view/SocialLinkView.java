package com.aionn.identity.application.dto.auth.view;

import java.time.LocalDateTime;

public record SocialLinkView(
        String socialLinkId,
        String userId,
        String provider,
        String providerUserId,
        LocalDateTime linkedAt) {
}
