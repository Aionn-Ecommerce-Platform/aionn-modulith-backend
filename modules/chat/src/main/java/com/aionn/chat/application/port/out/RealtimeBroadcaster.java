package com.aionn.chat.application.port.out;

import java.time.Instant;
import java.util.List;

import com.aionn.chat.application.dto.message.result.MessageResult;

public interface RealtimeBroadcaster {

    void broadcastMessage(MessageResult message, List<String> recipientIds);

    void broadcastConversationRead(String conversationId, String userId, Instant readAt);

    void broadcastTypingChange(String conversationId, String userId, boolean typing);

    void broadcastMessageRecalled(String conversationId, String messageId);
}

