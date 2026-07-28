package com.aionn.chat.application.usecase.autoreply;

import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.autoreply.UpdateAutoReplyInputPort;
import com.aionn.chat.application.service.AutoReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAutoReplyUseCase implements UpdateAutoReplyInputPort {

    private final AutoReplyService autoReplyService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional
    public AutoReplyResult execute(AutoReplyCommands.UpdateAutoReply command) {
        return chatResultMapper.toResult(autoReplyService.update(command));
    }
}
