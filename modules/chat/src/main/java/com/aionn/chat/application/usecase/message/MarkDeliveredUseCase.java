package com.aionn.chat.application.usecase.message;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.message.MarkDeliveredInputPort;
import com.aionn.chat.application.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkDeliveredUseCase implements MarkDeliveredInputPort {

    private final MessageService messageService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional
    public MessageResult execute(MessageCommands.DeliverMessage command) {
        return chatResultMapper.toResult(messageService.markDelivered(command));
    }
}
