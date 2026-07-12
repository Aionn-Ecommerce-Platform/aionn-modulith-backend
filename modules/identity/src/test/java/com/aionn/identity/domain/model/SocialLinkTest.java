package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLinkTest {

    @Test
    void createNewStampsCreatedAtWithinBoundaries() {
        Instant before = Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.SECONDS);

        SocialLink link = SocialLink.createNew("sa-1", "user-1", AuthProvider.GOOGLE, "google-123");

        Instant after = Instant.now(Clock.systemUTC()).plus(1, ChronoUnit.SECONDS);

        assertThat(link.socialAccountId()).isEqualTo("sa-1");
        assertThat(link.userId()).isEqualTo("user-1");
        assertThat(link.provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(link.providerUserId()).isEqualTo("google-123");
        assertThat(link.createdAt()).isBetween(before, after);
    }
}
