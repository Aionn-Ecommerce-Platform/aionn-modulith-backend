package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionTest {

    @Test
    void extendExpiryRejectsNonFutureExpiry() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                LocalDateTime.now(Clock.systemUTC()).minusMinutes(1),
                LocalDateTime.now(Clock.systemUTC()).minusMinutes(1),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(10));

        LocalDateTime nonFutureExpiry = LocalDateTime.now(Clock.systemUTC());

        assertThrows(IllegalArgumentException.class, () -> session.extendExpiry(nonFutureExpiry));
        assertDoesNotThrow(() -> session.extendExpiry(LocalDateTime.now(Clock.systemUTC()).plusMinutes(15)));
    }

    @Test
    void expiredSessionIsNotActive() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                LocalDateTime.now(Clock.systemUTC()).minusMinutes(2),
                LocalDateTime.now(Clock.systemUTC()).minusMinutes(2),
                LocalDateTime.now(Clock.systemUTC()).minusNanos(1));

        assertTrue(session.isExpired());
        assertFalse(session.isActive());
    }
}
