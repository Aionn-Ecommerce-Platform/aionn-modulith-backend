package com.aionn.identity.infrastructure.config.properties;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
@Builder
@ConfigurationProperties(prefix = "identity.agent")
public record AgentProperties(
                @DefaultValue("1") int keyExpiryYears) {
}

