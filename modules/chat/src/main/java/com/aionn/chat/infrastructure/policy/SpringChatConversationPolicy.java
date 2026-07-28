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
    public int getListDefaultLimit() {
        return properties.listDefaultLimit();
    }

    @Override
    public int getListMaxLimit() {
        return properties.listMaxLimit();
    }
}
