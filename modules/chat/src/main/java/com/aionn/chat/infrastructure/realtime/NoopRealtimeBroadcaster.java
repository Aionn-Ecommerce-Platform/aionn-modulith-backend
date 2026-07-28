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
        log.debug("[NOOP] broadcastMessage conversationId={} recipients={}",
                message.conversationId(), recipientIds);
    }

    @Override
    public void broadcastConversationRead(String conversationId, String userId, Instant readAt) {
        log.debug("[NOOP] broadcastConversationRead conversationId={} userId={} readAt={}",
                conversationId, userId, readAt);
    }

    @Override
    public void broadcastTypingChange(String conversationId, String userId, boolean typing) {
        log.debug("[NOOP] broadcastTypingChange conversationId={} userId={} typing={}",
                conversationId, userId, typing);
    }

    @Override
    public void broadcastMessageRecalled(String conversationId, String messageId) {
        log.debug("[NOOP] broadcastMessageRecalled conversationId={} messageId={}",
                conversationId, messageId);
    }
}
