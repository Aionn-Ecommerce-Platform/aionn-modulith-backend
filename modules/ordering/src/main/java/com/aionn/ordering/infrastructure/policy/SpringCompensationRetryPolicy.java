package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.CompensationRetryPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingCompensationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringCompensationRetryPolicy implements CompensationRetryPolicy {

    private final OrderingCompensationProperties properties;

    @Override
    public int maxAttempts() {
        return properties.maxAttempts();
    }

    @Override
    public int batchSize() {
        return properties.batchSize();
    }
}
