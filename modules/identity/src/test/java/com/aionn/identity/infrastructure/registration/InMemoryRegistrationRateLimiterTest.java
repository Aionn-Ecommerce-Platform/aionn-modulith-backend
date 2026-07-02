package com.aionn.identity.infrastructure.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRegistrationRateLimiterTest {

    private final InMemoryRegistrationRateLimiter limiter = new InMemoryRegistrationRateLimiter();

    @Test
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.check("ip", "1.1.1.1", 3, 60));
        }
    }

    @Test
    void rejectsWhenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login", "1.2.3.4", 3, 60);
        }
        assertFalse(limiter.check("login", "1.2.3.4", 3, 60));
    }

    @Test
    void differentScopesAreIsolated() {
        for (int i = 0; i < 3; i++) {
            limiter.check("scope-a", "1.1.1.1", 3, 60);
        }
        assertTrue(limiter.check("scope-b", "1.1.1.1", 3, 60));
    }

    @Test
    void differentKeysAreIsolated() {
        for (int i = 0; i < 3; i++) {
            limiter.check("ip", "1.1.1.1", 3, 60);
        }
        assertTrue(limiter.check("ip", "2.2.2.2", 3, 60));
    }

    @Test
    void blankKeyAlwaysAllowed() {
        assertTrue(limiter.check("ip", null, 1, 60));
        assertTrue(limiter.check("ip", "", 1, 60));
        assertTrue(limiter.check("ip", "   ", 1, 60));
    }
}
