package com.aionn.chat.application.port.in.conversation;

import com.aionn.chat.application.dto.conversation.result.ConversationResult;

import java.util.List;

public interface ListMyConversationsQueryPort {
    List<ConversationResult> execute(String userId, boolean includeArchived, int limit);
}
