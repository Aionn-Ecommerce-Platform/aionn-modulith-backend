package com.aionn.ucp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the expired checkout-session sweep, bound under
 * {@code aionn.ucp.session-cleanup.*}.
 */
@ConfigurationProperties(prefix = "aionn.ucp.session-cleanup")
public record UcpSessionCleanupProperties(
                @DefaultValue("true") boolean enabled,
                @DefaultValue("3600000") long delayMs,
                @DefaultValue("30") int retentionDays) {
}
