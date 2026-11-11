package com.aionn.platform.infrastructure.cache.invalidation;

public interface CacheInvalidationPublisher {

    void publish(CacheInvalidationMessage message);
}
