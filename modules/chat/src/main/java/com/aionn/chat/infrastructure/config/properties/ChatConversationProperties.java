package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.conversation")
public record ChatConversationProperties(
        @DefaultValue("50") int listDefaultLimit,
        @DefaultValue("100") int listMaxLimit) {
}
