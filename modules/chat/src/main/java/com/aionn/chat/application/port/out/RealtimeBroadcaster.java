package com.aionn.chat.application.port.out;

import com.aionn.chat.application.dto.message.result.MessageResult;

public interface RealtimeBroadcaster {

    void broadcastMessage(MessageResult message, java.util.List<String> recipientIds);

    void broadcastConversationRead(String conversationId, String userId, java.time.Instant readAt);

    void broadcastTypingChange(String conversationId, String userId, boolean typing);

    void broadcastMessageRecalled(String conversationId, String messageId);
}

