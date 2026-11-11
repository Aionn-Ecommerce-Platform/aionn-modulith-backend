package com.aionn.ordering.application.policy;

public interface RefundRetryPolicy {

    int maxAttempts();

    int batchSize();
}
