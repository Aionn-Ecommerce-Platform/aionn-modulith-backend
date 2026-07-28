package com.aionn.chat.infrastructure.realtime;

import com.aionn.chat.application.dto.message.result.MessageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RealtimeBroadcasterTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final String TOPIC = "/topic/conversations/conv-1";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private static MessageResult message() {
        return new MessageResult("msg-1", "conv-1", "buyer-1", "BUYER", "TEXT", "Hello",
                Map.of(), "SENT", Set.of(), Set.of(), false, NOW, NOW);
    }

    @Test
    void stompSendsOneUserQueueFramePerRecipient() {
        new StompRealtimeBroadcaster(messagingTemplate)
                .broadcastMessage(message(), List.of("mer-1", "cs-1"));

        verify(messagingTemplate).convertAndSendToUser("mer-1", "/queue/messages", message());
        verify(messagingTemplate).convertAndSendToUser("cs-1", "/queue/messages", message());
    }

    @Test
    void stompSendsNothingWhenNoRecipients() {
        new StompRealtimeBroadcaster(messagingTemplate).broadcastMessage(message(), List.of());

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void stompPublishesReadReceiptOnConversationTopic() {
        new StompRealtimeBroadcaster(messagingTemplate)
                .broadcastConversationRead("conv-1", "buyer-1", NOW);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(TOPIC), captor.capture());
        assertThat(asMap(captor.getValue()))
                .containsEntry("type", "CONVERSATION_READ")
                .containsEntry("userId", "buyer-1")
                .containsEntry("readAt", NOW.toString());
    }

    @Test
    void stompPublishesTypingOnConversationTopic() {
        new StompRealtimeBroadcaster(messagingTemplate)
                .broadcastTypingChange("conv-1", "buyer-1", true);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(TOPIC), captor.capture());
        assertThat(asMap(captor.getValue()))
                .containsEntry("type", "TYPING")
                .containsEntry("typing", true);
    }

    @Test
    void stompPublishesRecallOnConversationTopic() {
        new StompRealtimeBroadcaster(messagingTemplate)
                .broadcastMessageRecalled("conv-1", "msg-1");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(TOPIC), captor.capture());
        assertThat(asMap(captor.getValue()))
                .containsEntry("type", "MESSAGE_RECALLED")
                .containsEntry("messageId", "msg-1");
    }

    @Test
    void noopBroadcasterSwallowsEverything() {
        NoopRealtimeBroadcaster noop = new NoopRealtimeBroadcaster();

        assertThatCode(() -> {
            noop.broadcastMessage(message(), List.of("mer-1"));
            noop.broadcastConversationRead("conv-1", "buyer-1", NOW);
            noop.broadcastTypingChange("conv-1", "buyer-1", true);
            noop.broadcastMessageRecalled("conv-1", "msg-1");
        }).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object payload) {
        return (Map<String, Object>) payload;
    }
}
