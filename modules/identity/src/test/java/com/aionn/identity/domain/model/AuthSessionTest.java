package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSessionTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, java.time.ZoneOffset.UTC);

    @Test
    void extendExpiryRejectsNonFutureExpiry() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                NOW.minus(Duration.ofMinutes(1)),
                NOW.minus(Duration.ofMinutes(1)),
                NOW.plus(Duration.ofMinutes(10)));

        Instant nonFutureExpiry = NOW;

        assertThrows(IllegalArgumentException.class, () -> session.extendExpiry(nonFutureExpiry, CLOCK));
        assertDoesNotThrow(() -> session.extendExpiry(NOW.plus(Duration.ofMinutes(15)), CLOCK));
    }

    @Test
    void expiredSessionIsNotActive() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                NOW.minus(Duration.ofMinutes(2)),
                NOW.minus(Duration.ofMinutes(2)),
                NOW.minusNanos(1));

        assertThat(session.isExpired(CLOCK)).isTrue();
        assertThat(session.isActive(CLOCK)).isFalse();
    }
}
