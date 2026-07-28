package com.aionn.chat.application.usecase.message;

import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.policy.ChatMessagePolicy;
import com.aionn.chat.application.port.in.message.ListMessagesQueryPort;
import com.aionn.chat.application.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMessagesUseCase implements ListMessagesQueryPort {

    private final MessageService messageService;
    private final ChatMessagePolicy messagePolicy;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MessageResult> execute(String userId, String conversationId, Instant before, int limit) {
        int safeLimit = Math.clamp(limit, 1, messagePolicy.getListMaxLimit());
        if (before == null) {
            return chatResultMapper.toMessageResults(
                    messageService.listLatest(userId, conversationId, safeLimit));
        }
        return chatResultMapper.toMessageResults(
                messageService.listBefore(userId, conversationId, before, safeLimit));
    }
}
