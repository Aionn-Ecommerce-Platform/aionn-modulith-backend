package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.defaults")
public record NotificationDefaultsProperties(
        @DefaultValue("vi-VN") String locale,
        @DefaultValue("3") int maxRetryAttempts) {
}
