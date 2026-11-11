package com.aionn.ordering.application.policy;

public interface CompensationRetryPolicy {

    int maxAttempts();

    int batchSize();
}
