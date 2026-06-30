package com.aionn.sharedkernel.infrastructure.cache.invalidation;

public record CacheInvalidationMessage(
                String namespace,
                String key,
                String origin,
                boolean evictAll) {
}
