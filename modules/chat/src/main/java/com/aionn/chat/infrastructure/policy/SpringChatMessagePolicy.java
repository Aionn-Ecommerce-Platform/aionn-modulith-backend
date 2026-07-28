package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.application.policy.ChatMessagePolicy;
import com.aionn.chat.infrastructure.config.properties.ChatMessageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SpringChatMessagePolicy implements ChatMessagePolicy {

    private final ChatMessageProperties properties;

    @Override
    public int getMaxTextLength() {
        return properties.maxLength();
    }

    @Override
    public Duration getRecallWindow() {
        return Duration.ofSeconds(properties.recallWindowSeconds());
    }

    @Override
    public int getListDefaultLimit() {
        return properties.listDefaultLimit();
    }

    @Override
    public int getListMaxLimit() {
        return properties.listMaxLimit();
    }
}
