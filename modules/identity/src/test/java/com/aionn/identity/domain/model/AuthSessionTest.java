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

    @Test
    void extendExpiryRejectsNonFutureExpiry() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                Instant.now(Clock.systemUTC()).minus(Duration.ofMinutes(1)),
                Instant.now(Clock.systemUTC()).minus(Duration.ofMinutes(1)),
                Instant.now(Clock.systemUTC()).plus(Duration.ofMinutes(10)));

        Instant nonFutureExpiry = Instant.now(Clock.systemUTC());

        assertThrows(IllegalArgumentException.class, () -> session.extendExpiry(nonFutureExpiry));
        assertDoesNotThrow(() -> session.extendExpiry(Instant.now(Clock.systemUTC()).plus(Duration.ofMinutes(15))));
    }

    @Test
    void expiredSessionIsNotActive() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                Instant.now(Clock.systemUTC()).minus(Duration.ofMinutes(2)),
                Instant.now(Clock.systemUTC()).minus(Duration.ofMinutes(2)),
                Instant.now(Clock.systemUTC()).minusNanos(1));

        assertThat(session.isExpired()).isTrue();
        assertThat(session.isActive()).isFalse();
    }
}
