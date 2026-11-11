package com.aionn.platform.infrastructure.cache.invalidation;

public record CacheInvalidationMessage(
                String namespace,
                String key,
                String origin,
                boolean evictAll) {
}
