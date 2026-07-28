package com.aionn.chat.application.usecase.message;

import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.message.GetMessageQueryPort;
import com.aionn.chat.application.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMessageUseCase implements GetMessageQueryPort {

    private final MessageService messageService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional(readOnly = true)
    public MessageResult execute(String userId, String messageId) {
        return chatResultMapper.toResult(messageService.getForUser(userId, messageId));
    }
}
