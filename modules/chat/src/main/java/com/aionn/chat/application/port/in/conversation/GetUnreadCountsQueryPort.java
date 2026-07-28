package com.aionn.chat.application.port.in.conversation;

import java.util.Map;

public interface GetUnreadCountsQueryPort {
    Map<String, Long> execute(String userId);
}
