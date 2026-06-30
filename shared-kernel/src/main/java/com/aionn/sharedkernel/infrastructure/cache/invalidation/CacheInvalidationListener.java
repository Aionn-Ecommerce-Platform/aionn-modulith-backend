package com.aionn.sharedkernel.infrastructure.cache.invalidation;

import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TwoTierCacheRegistry registry;
    private final String origin;

    public CacheInvalidationListener(
            ObjectMapper objectMapper, TwoTierCacheRegistry registry, String origin) {
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.origin = origin;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheInvalidationMessage payload = objectMapper.readValue(
                    message.getBody(), CacheInvalidationMessage.class);
            if (origin != null && origin.equals(payload.origin())) {
                return;
            }
            registry.find(payload.namespace()).ifPresent(cache -> {
                if (payload.evictAll()) {
                    cache.invalidateAllLocal();
                } else if (payload.key() != null) {
                    cache.invalidateLocal(payload.key());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to handle cache invalidation message: {}", ex.getMessage());
        }
    }
}
