package com.aionn.chat.infrastructure.config;

import com.aionn.chat.infrastructure.config.properties.ChatAutoReplyProperties;
import com.aionn.chat.infrastructure.config.properties.ChatConversationProperties;
import com.aionn.chat.infrastructure.config.properties.ChatMessageProperties;
import com.aionn.chat.infrastructure.config.properties.ChatPresenceProperties;
import com.aionn.chat.infrastructure.config.properties.ChatRealtimeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ChatRealtimeProperties.class,
        ChatPresenceProperties.class,
        ChatMessageProperties.class,
        ChatConversationProperties.class,
        ChatAutoReplyProperties.class
})
public class ChatPropertiesConfig {
}
