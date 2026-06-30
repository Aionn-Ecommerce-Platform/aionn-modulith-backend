package com.aionn.sharedkernel.infrastructure.cache.invalidation;

public interface CacheInvalidationPublisher {

    void publish(CacheInvalidationMessage message);
}
