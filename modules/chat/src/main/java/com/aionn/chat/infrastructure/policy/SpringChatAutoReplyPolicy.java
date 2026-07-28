package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.application.policy.ChatAutoReplyPolicy;
import com.aionn.chat.infrastructure.config.properties.ChatAutoReplyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SpringChatAutoReplyPolicy implements ChatAutoReplyPolicy {

    private final ChatAutoReplyProperties properties;

    @Override
    public String getDefaultAwayMessage() {
        return properties.defaultAwayMessage();
    }

    @Override
    public ZoneId getDefaultTimezone() {
        return ZoneId.of(properties.defaultTimezone());
    }
}
