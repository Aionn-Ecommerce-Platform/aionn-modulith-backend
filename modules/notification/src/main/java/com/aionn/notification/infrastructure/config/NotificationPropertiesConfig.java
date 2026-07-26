package com.aionn.notification.infrastructure.config;

import com.aionn.notification.infrastructure.config.properties.NotificationDefaultsProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationEmailProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationPushProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationRecipientProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationRetryProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationSmsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        NotificationDefaultsProperties.class,
        NotificationEmailProperties.class,
        NotificationSmsProperties.class,
        NotificationPushProperties.class,
        NotificationRecipientProperties.class,
        NotificationRetryProperties.class
})
public class NotificationPropertiesConfig {
}
