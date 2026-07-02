package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.LocalDateTime;

public record SocialLinkResponse(
        String provider,
        String providerUserId,
        LocalDateTime linkedAt) {
}


