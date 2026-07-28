package com.aionn.chat.application.port.in.autoreply;

import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;

public interface GetAutoReplyQueryPort {
    AutoReplyResult execute(String ownerId, String merchantId);
}
