package com.aionn.sharedkernel.infrastructure.cache.core;

import java.util.Optional;
import java.util.function.Supplier;

public interface TwoTierCache<K, V> {

    String namespace();

    Optional<V> get(K key);

    V getOrLoad(K key, Supplier<V> loader);

    void put(K key, V value);

    void evict(K key);

    void evictAll();

    void invalidateLocal(K key);

    void invalidateAllLocal();
}
