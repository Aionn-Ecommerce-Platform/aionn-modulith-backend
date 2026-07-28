package com.aionn.chat.infrastructure.realtime;

import com.aionn.chat.application.port.out.PresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionListener {

    private final PresenceTracker presenceTracker;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user == null || sessionId == null) {
            log.debug("STOMP connect ignored - no principal/session");
            return;
        }
        presenceTracker.markOnline(user.getName(), sessionId);
        log.debug("Presence ON  user={} session={}", user.getName(), sessionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user == null || sessionId == null) {
            return;
        }
        presenceTracker.markOffline(user.getName(), sessionId);
        log.debug("Presence OFF user={} session={}", user.getName(), sessionId);
    }
}

