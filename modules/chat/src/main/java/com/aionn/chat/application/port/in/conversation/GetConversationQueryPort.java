package com.aionn.chat.application.port.in.conversation;

import com.aionn.chat.application.dto.conversation.result.ConversationResult;

public interface GetConversationQueryPort {
    ConversationResult execute(String userId, String conversationId);
}
