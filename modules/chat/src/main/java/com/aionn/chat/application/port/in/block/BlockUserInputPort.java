package com.aionn.chat.application.port.in.block;

import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.dto.block.result.BlockResult;

public interface BlockUserInputPort {
    BlockResult execute(BlockCommands.BlockUser command);
}
