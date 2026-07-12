package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthProvider;

import java.time.Clock;
import java.time.Instant;

public record SocialLink(
        String socialAccountId,
        String userId,
        AuthProvider provider,
        String providerUserId,
        Instant createdAt) {

    public static SocialLink createNew(
            String socialAccountId,
            String userId,
            AuthProvider provider,
            String providerUserId) {
        return createNew(socialAccountId, userId, provider, providerUserId, Clock.systemUTC());
    }

    public static SocialLink createNew(
            String socialAccountId,
            String userId,
            AuthProvider provider,
            String providerUserId,
            Clock clock) {
        return new SocialLink(socialAccountId, userId, provider, providerUserId, clock.instant());
    }
}

