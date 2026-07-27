package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.email")
public record NotificationEmailProperties(
        @DefaultValue("logging") String provider,
        @DefaultValue("") String from,
        @DefaultValue("Aionn notification") String defaultSubject) {
}
