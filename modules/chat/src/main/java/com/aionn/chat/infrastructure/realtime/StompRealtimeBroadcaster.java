package com.aionn.chat.infrastructure.realtime;

import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.port.out.RealtimeBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.realtime", name = "provider", havingValue = "stomp")
public class StompRealtimeBroadcaster implements RealtimeBroadcaster {

    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations/";
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONVERSATION_ID = "conversationId";
    private static final String KEY_USER_ID = "userId";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastMessage(MessageResult message, List<String> recipientIds) {
        for (String recipientId : recipientIds) {
            messagingTemplate.convertAndSendToUser(recipientId, "/queue/messages", message);
        }
    }

    @Override
    public void broadcastConversationRead(String conversationId, String userId, Instant readAt) {
        broadcastToConversation(conversationId, Map.of(
                KEY_TYPE, "CONVERSATION_READ",
                KEY_CONVERSATION_ID, conversationId,
                KEY_USER_ID, userId,
                "readAt", readAt.toString()));
    }

    @Override
    public void broadcastTypingChange(String conversationId, String userId, boolean typing) {
        broadcastToConversation(conversationId, Map.of(
                KEY_TYPE, "TYPING",
                KEY_CONVERSATION_ID, conversationId,
                KEY_USER_ID, userId,
                "typing", typing));
    }

    @Override
    public void broadcastMessageRecalled(String conversationId, String messageId) {
        broadcastToConversation(conversationId, Map.of(
                KEY_TYPE, "MESSAGE_RECALLED",
                KEY_CONVERSATION_ID, conversationId,
                "messageId", messageId));
    }

    private void broadcastToConversation(String conversationId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + conversationId, (Object) payload);
    }
}
