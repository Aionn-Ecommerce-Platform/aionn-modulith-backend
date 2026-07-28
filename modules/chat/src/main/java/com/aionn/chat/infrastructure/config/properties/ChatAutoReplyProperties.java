package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.auto-reply")
public record ChatAutoReplyProperties(
        @DefaultValue("Hien tai shop dang ngoai gio lam viec, se phan hoi ban som nhat.") String defaultAwayMessage,
        @DefaultValue("Asia/Ho_Chi_Minh") String defaultTimezone) {
}
