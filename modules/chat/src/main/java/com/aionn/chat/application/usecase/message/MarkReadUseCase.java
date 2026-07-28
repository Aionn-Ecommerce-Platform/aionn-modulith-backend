package com.aionn.chat.application.usecase.message;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.message.MarkReadInputPort;
import com.aionn.chat.application.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkReadUseCase implements MarkReadInputPort {

    private final MessageService messageService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional
    public MessageResult execute(MessageCommands.ReadMessage command) {
        return chatResultMapper.toResult(messageService.markRead(command));
    }
}
