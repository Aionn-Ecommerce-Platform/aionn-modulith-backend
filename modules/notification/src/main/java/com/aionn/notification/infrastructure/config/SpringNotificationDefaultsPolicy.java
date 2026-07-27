package com.aionn.notification.infrastructure.config;

import com.aionn.notification.application.policy.NotificationDefaultsPolicy;
import com.aionn.notification.infrastructure.config.properties.NotificationDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationDefaultsPolicy implements NotificationDefaultsPolicy {

    private final NotificationDefaultsProperties properties;

    @Override
    public String defaultLocale() {
        String locale = properties.locale();
        return (locale == null || locale.isBlank()) ? "vi-VN" : locale.trim();
    }

    @Override
    public int maxRetryAttempts() {
        int attempts = properties.maxRetryAttempts();
        return attempts <= 0 ? 3 : attempts;
    }
}
