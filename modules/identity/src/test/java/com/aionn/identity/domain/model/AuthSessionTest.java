package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10));

        assertThrows(IllegalArgumentException.class, () -> session.extendExpiry(LocalDateTime.now()));
    }

    @Test
    void expiredSessionIsNotActive() {
        AuthSession session = new AuthSession(
                "session-1",
                "user-1",
                "1.1.1.1",
                "ua",
                AuthSessionStatus.ACTIVE,
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().minusNanos(1));

        assertTrue(session.isExpired());
        assertFalse(session.isActive());
    }
}
