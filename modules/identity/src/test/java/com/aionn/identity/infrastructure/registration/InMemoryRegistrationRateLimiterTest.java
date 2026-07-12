package com.aionn.identity.infrastructure.registration;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRegistrationRateLimiterTest {

    private final InMemoryRegistrationRateLimiter limiter = new InMemoryRegistrationRateLimiter(java.time.Clock.systemUTC());

    @Test
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.check("ip", "1.1.1.1", 3, 60)).isTrue();
        }
    }

    @Test
    void rejectsWhenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login", "1.2.3.4", 3, 60);
        }
        assertThat(limiter.check("login", "1.2.3.4", 3, 60)).isFalse();
    }

    @Test
    void differentScopesAreIsolated() {
        for (int i = 0; i < 3; i++) {
            limiter.check("scope-a", "1.1.1.1", 3, 60);
        }
        assertThat(limiter.check("scope-b", "1.1.1.1", 3, 60)).isTrue();
    }

    @Test
    void differentKeysAreIsolated() {
        for (int i = 0; i < 3; i++) {
            limiter.check("ip", "1.1.1.1", 3, 60);
        }
        assertThat(limiter.check("ip", "2.2.2.2", 3, 60)).isTrue();
    }

    @Test
    void blankKeyAlwaysAllowed() {
        assertThat(limiter.check("ip", null, 1, 60)).isTrue();
        assertThat(limiter.check("ip", "", 1, 60)).isTrue();
        assertThat(limiter.check("ip", "   ", 1, 60)).isTrue();
    }
}
