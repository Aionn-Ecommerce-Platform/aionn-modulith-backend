package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.message")
public record ChatMessageProperties(
        @DefaultValue("4000") int maxLength,
        @DefaultValue("120") int recallWindowSeconds,
        @DefaultValue("30") int listDefaultLimit,
        @DefaultValue("100") int listMaxLimit) {
}
