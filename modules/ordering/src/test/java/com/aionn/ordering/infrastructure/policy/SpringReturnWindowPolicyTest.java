package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.SpringReturnWindowPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingReturnProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringReturnWindowPolicyTest {

    @Mock
    private OrderingReturnProperties properties;

    @InjectMocks
    private SpringReturnWindowPolicy policy;

    @Test
    void returnsWindowDurationFromProperties() {
        when(properties.windowDays()).thenReturn(7);

        Duration result = policy.windowDuration();

        assertThat(result).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void supports30DayReturnWindow() {
        when(properties.windowDays()).thenReturn(30);

        Duration result = policy.windowDuration();

        assertThat(result).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void returnsTrueWhenNowIsWithinWindow() {
        when(properties.windowDays()).thenReturn(7);
        Instant completedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-05T00:00:00Z");

        boolean result = policy.isWithinWindow(completedAt, now);

        assertThat(result).isTrue();
    }

    @Test
    void returnsTrueOnExactWindowBoundary() {
        when(properties.windowDays()).thenReturn(7);
        Instant completedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-08T00:00:00Z");

        boolean result = policy.isWithinWindow(completedAt, now);

        assertThat(result).isTrue();
    }

    @Test
    void returnsFalseWhenNowIsAfterWindow() {
        when(properties.windowDays()).thenReturn(7);
        Instant completedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-10T00:00:00Z");

        boolean result = policy.isWithinWindow(completedAt, now);

        assertThat(result).isFalse();
    }

    @Test
    void returnsFalseWhenCompletedAtIsNull() {
        Instant now = Instant.parse("2026-01-05T00:00:00Z");

        boolean result = policy.isWithinWindow(null, now);

        assertThat(result).isFalse();
    }

    @Test
    void returnsTrueForOrderCompletedOneSecondAgo() {
        when(properties.windowDays()).thenReturn(7);
        Instant completedAt = Instant.parse("2026-01-01T12:00:00Z");
        Instant now = Instant.parse("2026-01-01T12:00:01Z");

        boolean result = policy.isWithinWindow(completedAt, now);

        assertThat(result).isTrue();
    }

    @Test
    void handlesDifferentWindowDurations() {
        // 14-day window
        when(properties.windowDays()).thenReturn(14);
        Instant completedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-10T00:00:00Z");

        assertThat(policy.isWithinWindow(completedAt, now)).isTrue();

        // 30-day window
        when(properties.windowDays()).thenReturn(30);
        now = Instant.parse("2026-01-25T00:00:00Z");

        assertThat(policy.isWithinWindow(completedAt, now)).isTrue();
    }
}
