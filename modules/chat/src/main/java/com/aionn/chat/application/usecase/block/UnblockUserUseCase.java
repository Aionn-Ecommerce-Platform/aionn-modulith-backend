package com.aionn.chat.application.usecase.block;

import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.dto.block.result.BlockResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.block.UnblockUserInputPort;
import com.aionn.chat.application.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnblockUserUseCase implements UnblockUserInputPort {

    private final UserBlockService userBlockService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional
    public BlockResult execute(BlockCommands.UnblockUser command) {
        return chatResultMapper.toResult(userBlockService.unblock(command));
    }
}
