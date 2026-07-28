package com.aionn.chat.application.usecase.conversation;

import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.policy.ChatConversationPolicy;
import com.aionn.chat.application.port.in.conversation.ListMyConversationsQueryPort;
import com.aionn.chat.application.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyConversationsUseCase implements ListMyConversationsQueryPort {

    private final ConversationService conversationService;
    private final ChatConversationPolicy conversationPolicy;

    @Override
    public List<ConversationResult> execute(String userId, boolean includeArchived, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), conversationPolicy.getListMaxLimit());
        return conversationService.listForUser(userId, includeArchived, safeLimit);
    }
}
