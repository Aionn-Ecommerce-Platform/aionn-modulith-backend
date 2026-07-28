package com.aionn.chat.application.usecase.autoreply;

import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.in.autoreply.GetAutoReplyQueryPort;
import com.aionn.chat.application.service.AutoReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAutoReplyUseCase implements GetAutoReplyQueryPort {

    private final AutoReplyService autoReplyService;
    private final ChatResultMapper chatResultMapper;

    @Override
    @Transactional(readOnly = true)
    public AutoReplyResult execute(String ownerId, String merchantId) {
        return chatResultMapper.toResult(autoReplyService.get(ownerId, merchantId));
    }
}
