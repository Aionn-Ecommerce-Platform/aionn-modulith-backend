package com.aionn.identity.adapter.rest.dto.auth.response;

import java.time.Instant;

public record SocialLinkResponse(
        String provider,
        String providerUserId,
        Instant linkedAt) {
}


