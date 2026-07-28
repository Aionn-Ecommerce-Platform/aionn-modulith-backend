package com.aionn.chat.application.port.in.block;

import com.aionn.chat.application.dto.block.result.BlockResult;

import java.util.List;

public interface ListMyBlocksQueryPort {
    List<BlockResult> execute(String userId);
}
