package com.aionn.chat.application.policy;

import java.time.ZoneId;

public interface ChatAutoReplyPolicy {

    String getDefaultAwayMessage();

    ZoneId getDefaultTimezone();
}
