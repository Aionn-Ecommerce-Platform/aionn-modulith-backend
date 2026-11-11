package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tuning for the background worker that materialises personal data exports.
 *
 * <p>Exports are built off the request thread: the snapshot spans nine tables and is serialised to JSON,
 * which is too much work to hold a user-facing POST open for.
 */
@ConfigurationProperties(prefix = "identity.data-export")
public record DataExportProperties(
        @DefaultValue("10") int batchSize) {
}
