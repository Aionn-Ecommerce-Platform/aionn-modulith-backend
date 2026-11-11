package com.aionn.chat.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.conversation")
public record ChatConversationProperties(
        @Min(1) @Max(1000) @DefaultValue("50") int listDefaultLimit,
        @Min(1) @Max(1000) @DefaultValue("100") int listMaxLimit,
        @Min(1) @Max(10_000) @DefaultValue("100") int unreadScanLimit) {
}
