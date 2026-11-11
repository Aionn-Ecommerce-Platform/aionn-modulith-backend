package com.aionn.chat.infrastructure.presence;

import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.chat.infrastructure.config.properties.ChatPresenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.presence", name = "provider", havingValue = "redis")
public class RedisPresenceTracker implements PresenceTracker {

    private static final String KEY_PREFIX = "chat:presence:";

    private final StringRedisTemplate redis;
    private final ChatPresenceProperties properties;
    private final ConcurrentMap<String, Set<String>> localSessions = new ConcurrentHashMap<>();

    @Override
    public void markOnline(String userId, String sessionId) {
        localSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        String key = key(userId);
        redis.opsForSet().add(key, sessionId);
        redis.expire(key, Duration.ofSeconds(properties.ttlSeconds()));
    }

    @Override
    public void markOffline(String userId, String sessionId) {
        Set<String> sessions = localSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                localSessions.remove(userId, sessions);
            }
        }
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

    /** Keeps long-lived WebSocket sessions from disappearing between STOMP connect/disconnect. */
    @Scheduled(fixedDelayString = "${chat.presence.refresh-ms:60000}")
    public void refreshLocalPresence() {
        localSessions.forEach((userId, sessionIds) -> {
            if (sessionIds.isEmpty()) {
                localSessions.remove(userId, sessionIds);
                return;
            }
            // Re-add the ids as well: this repairs a lost Redis key without waiting for reconnect.
            redis.opsForSet().add(key(userId), sessionIds.toArray(String[]::new));
            redis.expire(key(userId), Duration.ofSeconds(properties.ttlSeconds()));
        });
    }

    private static String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
