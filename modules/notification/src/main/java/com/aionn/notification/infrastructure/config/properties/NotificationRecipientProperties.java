package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.recipient")
public record NotificationRecipientProperties(
        @DefaultValue("stub") String provider) {
}
