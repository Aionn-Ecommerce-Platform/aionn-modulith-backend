package com.aionn.chat.application.port.in.block;

import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.dto.block.result.BlockResult;

public interface UnblockUserInputPort {
    BlockResult execute(BlockCommands.UnblockUser command);
}
