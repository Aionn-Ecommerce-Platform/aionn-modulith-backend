package com.aionn.chat.application.port.out;

import java.util.Set;

public interface PresenceTracker {

    void markOnline(String userId, String sessionId);

    void markOffline(String userId, String sessionId);

    boolean isOnline(String userId);

    Set<String> filterOnline(Set<String> userIds);
}

