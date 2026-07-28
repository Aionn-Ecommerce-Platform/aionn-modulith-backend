package com.aionn.chat.application.port.in.message;

import com.aionn.chat.application.dto.message.result.MessageResult;

public interface GetMessageQueryPort {
    MessageResult execute(String userId, String messageId);
}
