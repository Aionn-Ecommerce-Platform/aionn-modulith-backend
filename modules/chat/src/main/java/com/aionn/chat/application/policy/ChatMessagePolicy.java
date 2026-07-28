package com.aionn.chat.application.policy;

import java.time.Duration;

public interface ChatMessagePolicy {

    int getMaxTextLength();

    Duration getRecallWindow();

    int getListDefaultLimit();

    int getListMaxLimit();
}
