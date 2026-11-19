package com.aionn.chat.infrastructure.presence;

import com.aionn.chat.application.port.out.PresenceTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.presence", name = "provider", havingValue = "memory")
public class InMemoryPresenceTracker implements PresenceTracker {

    private final Clock clock;
    private final ConcurrentMap<String, Set<String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    @Override
    public void markOnline(String userId, String sessionId) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        lastSeen.put(userId, Instant.now(clock));
    }

    @Override
    public void markOffline(String userId, String sessionId) {
        Set<String> set = sessions.get(userId);
        if (set == null)
            return;
        set.remove(sessionId);
        if (set.isEmpty()) {
            sessions.remove(userId);
        }
        lastSeen.put(userId, Instant.now(clock));
    }

    @Override
    public boolean isOnline(String userId) {
        Set<String> set = sessions.get(userId);
        return set != null && !set.isEmpty();
    }

    @Override
    public Optional<Instant> lastSeenAt(String userId) {
        return Optional.ofNullable(lastSeen.get(userId));
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
}

