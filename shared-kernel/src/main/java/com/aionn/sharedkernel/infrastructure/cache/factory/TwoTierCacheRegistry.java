package com.aionn.sharedkernel.infrastructure.cache.factory;

import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TwoTierCacheRegistry {

    private final Map<String, TwoTierCache<String, ?>> caches = new ConcurrentHashMap<>();

    public void register(TwoTierCache<String, ?> cache) {
        TwoTierCache<String, ?> existing = caches.putIfAbsent(cache.namespace(), cache);
        if (existing != null) {
            throw new IllegalStateException("Duplicate two-tier cache namespace: " + cache.namespace());
        }
    }

    public Optional<TwoTierCache<String, ?>> find(String namespace) {
        return Optional.ofNullable(caches.get(namespace));
    }

    public Map<String, TwoTierCache<String, ?>> all() {
        return Map.copyOf(caches);
    }
}
