package com.aionn.chat.application.policy;

import java.time.Duration;

public interface ChatMessagePolicy {

    int maxTextLength();

    Duration recallWindow();

    int listDefaultLimit();

    int listMaxLimit();
}
