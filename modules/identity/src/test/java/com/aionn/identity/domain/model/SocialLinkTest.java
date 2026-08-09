package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLinkTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, java.time.ZoneOffset.UTC);

    @Test
    void createNewStampsCreatedAtWithinBoundaries() {
        SocialLink link = SocialLink.createNew("sa-1", "user-1", AuthProvider.GOOGLE, "google-123", CLOCK);

        assertThat(link.socialAccountId()).isEqualTo("sa-1");
        assertThat(link.userId()).isEqualTo("user-1");
        assertThat(link.provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(link.providerUserId()).isEqualTo("google-123");
        assertThat(link.createdAt()).isEqualTo(NOW);
    }
}
