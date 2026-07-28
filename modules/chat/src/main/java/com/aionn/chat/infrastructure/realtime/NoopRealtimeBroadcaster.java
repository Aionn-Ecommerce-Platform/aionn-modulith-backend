package com.aionn.chat.infrastructure.realtime;

import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.port.out.RealtimeBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "chat.realtime", name = "provider", havingValue = "noop")
public class NoopRealtimeBroadcaster implements RealtimeBroadcaster {

    @Override
    public void broadcastMessage(MessageResult message, List<String> recipientIds) {
        log.debug("[NOOP] broadcastMessage to {}", recipientIds);
    }

    @Override
    public void broadcastConversationRead(String conversationId, String userId, Instant readAt) {
    }

    @Override
    public void broadcastTypingChange(String conversationId, String userId, boolean typing) {
    }

    @Override
    public void broadcastMessageRecalled(String conversationId, String messageId) {
    }
}

