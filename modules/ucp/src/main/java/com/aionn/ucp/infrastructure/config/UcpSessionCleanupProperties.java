package com.aionn.ucp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the expired checkout-session sweep. Bound under
 * {@code aionn.ucp.session-cleanup.*}; also lets the IDE/tooling recognise these
 * keys (they're consumed via property placeholders in
 * {@code UcpCheckoutSessionCleanupScheduler}).
 */
@ConfigurationProperties(prefix = "aionn.ucp.session-cleanup")
public record UcpSessionCleanupProperties(
                @DefaultValue("true") boolean enabled,
                @DefaultValue("3600000") long delayMs) {
}
