package com.aionn.chat.infrastructure.config;

import com.aionn.chat.infrastructure.config.properties.ChatCloudinaryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatCloudinaryProperties.class)
public class ChatMediaConfig {
}
