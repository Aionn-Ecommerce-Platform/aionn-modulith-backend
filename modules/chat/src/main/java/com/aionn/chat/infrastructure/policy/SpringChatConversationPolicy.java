package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.application.policy.ChatConversationPolicy;
import com.aionn.chat.infrastructure.config.properties.ChatConversationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringChatConversationPolicy implements ChatConversationPolicy {

    private final ChatConversationProperties properties;

    @Override
    public int listDefaultLimit() {
        return properties.listDefaultLimit();
    }

    @Override
    public int listMaxLimit() {
        return properties.listMaxLimit();
    }

    @Override
    public int unreadScanLimit() {
        return properties.unreadScanLimit();
    }
}
