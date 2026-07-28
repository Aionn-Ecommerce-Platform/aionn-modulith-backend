package com.aionn.chat.application.usecase.conversation;

import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.port.in.conversation.GetConversationQueryPort;
import com.aionn.chat.application.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetConversationUseCase implements GetConversationQueryPort {

    private final ConversationService conversationService;

    @Override
    public ConversationResult execute(String userId, String conversationId) {
        return conversationService.getForUser(userId, conversationId);
    }
}
