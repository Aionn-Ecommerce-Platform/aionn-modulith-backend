package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ordering.return")
public record OrderingReturnProperties(
        @DefaultValue("7") int windowDays) {
}
