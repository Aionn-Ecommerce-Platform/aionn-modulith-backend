package com.aionn.identity.application.port.out.social;

public record SocialUserProfile(
        String providerUserId,
        String email,
        String displayName) {
}
