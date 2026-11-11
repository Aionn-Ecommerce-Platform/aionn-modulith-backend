package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.RefundRetryPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingReturnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringRefundRetryPolicy implements RefundRetryPolicy {

    private final OrderingReturnProperties properties;

    @Override
    public int maxAttempts() {
        return properties.refundMaxAttempts();
    }

    @Override
    public int batchSize() {
        return properties.refundBatchSize();
    }
}
