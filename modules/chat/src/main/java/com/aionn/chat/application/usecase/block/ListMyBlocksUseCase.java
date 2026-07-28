package com.aionn.chat.application.usecase.block;

import com.aionn.chat.application.dto.block.result.BlockResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.block.ListMyBlocksQueryPort;
import com.aionn.chat.application.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyBlocksUseCase implements ListMyBlocksQueryPort {

    private final UserBlockService userBlockService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BlockResult> execute(String userId) {
        return chatResultMapper.toBlockResults(userBlockService.listMyBlocks(userId));
    }
}
