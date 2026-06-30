package com.aionn.sharedkernel.infrastructure.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheRegistry;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationListener;
import com.aionn.sharedkernel.infrastructure.cache.invalidation.CacheInvalidationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

class CacheInvalidationListenerTest {

    @Test
    void cacheInvalidationListenerIgnoresOwnOriginAndHandlesEvictBranches() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TwoTierCacheRegistry registry = mock(TwoTierCacheRegistry.class);
        @SuppressWarnings("unchecked")
        TwoTierCache<String, Object> cache = mock(TwoTierCache.class);
        Message message = mock(Message.class);

        when(registry.find("catalog")).thenReturn(Optional.of(cache));

        CacheInvalidationMessage evictOne = new CacheInvalidationMessage("catalog", "sku-1", "node-b", false);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(evictOne));
        new CacheInvalidationListener(objectMapper, registry, "node-a").onMessage(message, null);
        verify(cache).invalidateLocal("sku-1");

        CacheInvalidationMessage evictAll = new CacheInvalidationMessage("catalog", null, "node-c", true);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(evictAll));
        new CacheInvalidationListener(objectMapper, registry, "node-a").onMessage(message, null);
        verify(cache).invalidateAllLocal();

        CacheInvalidationMessage sameOrigin = new CacheInvalidationMessage("catalog", "sku-2", "node-a", false);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(sameOrigin));
        new CacheInvalidationListener(objectMapper, registry, "node-a").onMessage(message, null);
        verify(cache, never()).invalidateLocal("sku-2");
    }

    @Test
    void cacheInvalidationListenerSwallowsMalformedMessagesAndMissingCache() {
        ObjectMapper objectMapper = new ObjectMapper();
        TwoTierCacheRegistry registry = mock(TwoTierCacheRegistry.class);
        Message message = mock(Message.class);

        when(message.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));
        new CacheInvalidationListener(objectMapper, registry, "node-a").onMessage(message, null);

        when(message.getBody()).thenReturn("{\"namespace\":\"missing\",\"key\":\"sku-1\",\"origin\":\"node-b\",\"evictAll\":false}"
                .getBytes(StandardCharsets.UTF_8));
        when(registry.find("missing")).thenReturn(Optional.empty());
        new CacheInvalidationListener(objectMapper, registry, "node-a").onMessage(message, null);
        verify(registry).find("missing");
    }
}
