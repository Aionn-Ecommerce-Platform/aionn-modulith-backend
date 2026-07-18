package com.aionn.ordering.application.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingReturnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SpringReturnWindowPolicy implements ReturnWindowPolicy {

    private final OrderingReturnProperties properties;

    @Override
    public Duration windowDuration() {
        return Duration.ofDays(properties.windowDays());
    }

    @Override
    public boolean isWithinWindow(Instant completedAt, Instant now) {
        if (completedAt == null) {
            return false;
        }
        return !now.isAfter(completedAt.plus(windowDuration()));
    }
}
