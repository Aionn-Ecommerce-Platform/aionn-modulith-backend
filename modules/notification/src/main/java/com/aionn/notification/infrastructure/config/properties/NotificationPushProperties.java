package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.push")
public record NotificationPushProperties(
        @DefaultValue("logging") String provider) {
}
