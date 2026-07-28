package com.aionn.chat.infrastructure.realtime;

import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.sharedkernel.integration.port.identity.AccessTokenVerifierPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionListenerTest {

    @Mock
    private PresenceTracker presenceTracker;
    @Mock
    private AccessTokenVerifierPort accessTokenVerifier;

    private static Message<byte[]> frame(StompCommand command, String sessionId, String userName) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        if (userName != null) {
            accessor.setUser((Principal) () -> userName);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectMarksUserOnline() {
        ChatSessionListener listener = new ChatSessionListener(presenceTracker);

        listener.onConnect(new SessionConnectedEvent(this,
                frame(StompCommand.CONNECTED, "s1", "user-1")));

        verify(presenceTracker).markOnline("user-1", "s1");
    }

    @Test
    void connectWithoutPrincipalIsIgnored() {
        ChatSessionListener listener = new ChatSessionListener(presenceTracker);

        listener.onConnect(new SessionConnectedEvent(this,
                frame(StompCommand.CONNECTED, "s1", null)));

        verifyNoInteractions(presenceTracker);
    }

    @Test
    void disconnectMarksUserOffline() {
        ChatSessionListener listener = new ChatSessionListener(presenceTracker);

        listener.onDisconnect(new SessionDisconnectEvent(this,
                frame(StompCommand.DISCONNECT, "s1", "user-1"), "s1", null));

        verify(presenceTracker).markOffline("user-1", "s1");
    }

    @Test
    void disconnectWithoutPrincipalIsIgnored() {
        ChatSessionListener listener = new ChatSessionListener(presenceTracker);

        listener.onDisconnect(new SessionDisconnectEvent(this,
                frame(StompCommand.DISCONNECT, "s1", null), "s1", null));

        verifyNoInteractions(presenceTracker);
    }

    @Test
    void principalResolverReturnsUserIdFromIdentity() {
        when(accessTokenVerifier.verifyAndExtractUserId("Bearer token"))
                .thenReturn(Optional.of("user-1"));

        assertThat(new StompPrincipalResolver(accessTokenVerifier).resolveUserId("Bearer token"))
                .isEqualTo("user-1");
    }

    @Test
    void principalResolverReturnsNullWhenTokenRejected() {
        when(accessTokenVerifier.verifyAndExtractUserId("bad")).thenReturn(Optional.empty());

        assertThat(new StompPrincipalResolver(accessTokenVerifier).resolveUserId("bad")).isNull();
    }
}
