package com.aionn.chat.application.usecase.conversation;

import com.aionn.chat.application.port.in.conversation.GetUnreadCountsQueryPort;
import com.aionn.chat.application.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetUnreadCountsUseCase implements GetUnreadCountsQueryPort {

    private final ConversationService conversationService;

    @Override
    public Map<String, Long> execute(String userId) {
        return conversationService.getUnreadCounts(userId);
    }
}
