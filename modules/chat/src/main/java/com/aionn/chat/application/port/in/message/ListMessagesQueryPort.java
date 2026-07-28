package com.aionn.chat.application.port.in.message;

import com.aionn.chat.application.dto.message.result.MessageResult;

import java.time.Instant;
import java.util.List;

public interface ListMessagesQueryPort {
    List<MessageResult> execute(String userId, String conversationId, Instant before, int limit);
}
