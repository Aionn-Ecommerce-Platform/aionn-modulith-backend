package com.aionn.chat.infrastructure.presence;

import com.aionn.chat.application.port.out.PresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.presence", name = "provider", havingValue = "redis")
public class RedisPresenceTracker implements PresenceTracker {

    private static final String KEY_PREFIX = "chat:presence:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    @Override
    public void markOnline(String userId, String sessionId) {
        String key = key(userId);
        redis.opsForSet().add(key, sessionId);
        redis.expire(key, TTL);
    }

    @Override
    public void markOffline(String userId, String sessionId) {
        String key = key(userId);
        redis.opsForSet().remove(key, sessionId);
        Long size = redis.opsForSet().size(key);
        if (size == null || size == 0) {
            redis.delete(key);
        }
    }

    @Override
    public boolean isOnline(String userId) {
        Long size = redis.opsForSet().size(key(userId));
        return size != null && size > 0;
    }

    @Override
    public Set<String> filterOnline(Set<String> userIds) {
        Set<String> online = new HashSet<>();
        for (String id : userIds) {
            if (isOnline(id))
                online.add(id);
        }
        return online;
    }

    private static String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
