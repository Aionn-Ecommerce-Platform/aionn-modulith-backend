package com.aionn.ordering.application.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingAutoCancelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SpringAutoCancelPolicy implements AutoCancelPolicy {

    private final OrderingAutoCancelProperties properties;

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(properties.timeoutMinutes());
    }

    @Override
    public int batchSize() {
        return Math.max(1, properties.batchSize());
    }

    @Override
    public Instant cutoff(Instant now) {
        return now.minus(timeout());
    }
}
