package com.aionn.chat.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface PresenceTracker {

    void markOnline(String userId, String sessionId);

    void markOffline(String userId, String sessionId);

    boolean isOnline(String userId);

    Optional<Instant> lastSeenAt(String userId);

    Set<String> filterOnline(Set<String> userIds);
}

