package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLinkTest {

    @Test
    void createNewPopulatesFieldsAndStampsUtcNow() {
        LocalDateTime before = LocalDateTime.now(java.time.Clock.systemUTC()).minus(1, ChronoUnit.SECONDS);

        SocialLink link = SocialLink.createNew("sa-1", "user-1", AuthProvider.GOOGLE, "google-123");

        LocalDateTime after = LocalDateTime.now(java.time.Clock.systemUTC()).plus(1, ChronoUnit.SECONDS);

        assertThat(link.socialAccountId()).isEqualTo("sa-1");
        assertThat(link.userId()).isEqualTo("user-1");
        assertThat(link.provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(link.providerUserId()).isEqualTo("google-123");
        assertThat(link.createdAt()).isBetween(before, after);
    }

    @Test
    void recordConstructorPreservesProvidedFields() {
        LocalDateTime ts = LocalDateTime.of(2024, 5, 1, 12, 0);
        SocialLink link = new SocialLink("sa-9", "user-9", AuthProvider.GOOGLE, "gid-9", ts);

        assertThat(link.createdAt()).isEqualTo(ts);
    }
}
