package com.aionn.platform.infrastructure.cache.core;

import java.time.Duration;

public record TwoTierCacheProperties(
        String namespace,
        Duration l1Ttl,
        long l1MaxSize,
        Duration l2Ttl,
        int evictionScanBatchSize) {

    public TwoTierCacheProperties(String namespace, Duration l1Ttl, long l1MaxSize, Duration l2Ttl) {
        this(namespace, l1Ttl, l1MaxSize, l2Ttl, 200);
    }

    public TwoTierCacheProperties {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (l1Ttl == null || l1Ttl.isNegative() || l1Ttl.isZero()) {
            throw new IllegalArgumentException("l1Ttl must be positive");
        }
        if (l1MaxSize <= 0) {
            throw new IllegalArgumentException("l1MaxSize must be positive");
        }
        if (l2Ttl == null || l2Ttl.isNegative() || l2Ttl.isZero()) {
            throw new IllegalArgumentException("l2Ttl must be positive");
        }
        if (evictionScanBatchSize <= 0) {
            throw new IllegalArgumentException("evictionScanBatchSize must be positive");
        }
    }
}
