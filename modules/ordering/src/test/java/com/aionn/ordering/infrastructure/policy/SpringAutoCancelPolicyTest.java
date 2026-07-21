package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.SpringAutoCancelPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingAutoCancelProperties;
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
class SpringAutoCancelPolicyTest {

    @Mock
    private OrderingAutoCancelProperties properties;

    @InjectMocks
    private SpringAutoCancelPolicy policy;

    @Test
    void returnsTimeoutFromProperties() {
        when(properties.timeoutMinutes()).thenReturn(30);

        Duration result = policy.timeout();

        assertThat(result).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void returnsBatchSizeFromProperties() {
        when(properties.batchSize()).thenReturn(50);

        int result = policy.batchSize();

        assertThat(result).isEqualTo(50);
    }

    @Test
    void enforcesMinimumBatchSizeOfOne() {
        when(properties.batchSize()).thenReturn(0);

        int result = policy.batchSize();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void enforcesMinimumBatchSizeForNegativeValues() {
        when(properties.batchSize()).thenReturn(-10);

        int result = policy.batchSize();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void calculatesCutoffBySubtractingTimeoutFromNow() {
        when(properties.timeoutMinutes()).thenReturn(60);
        Instant now = Instant.parse("2026-01-01T12:00:00Z");

        Instant result = policy.cutoff(now);

        assertThat(result).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }

    @Test
    void calculatesCutoffWith30MinuteTimeout() {
        when(properties.timeoutMinutes()).thenReturn(30);
        Instant now = Instant.parse("2026-01-01T12:00:00Z");

        Instant result = policy.cutoff(now);

        assertThat(result).isEqualTo(Instant.parse("2026-01-01T11:30:00Z"));
    }
}
