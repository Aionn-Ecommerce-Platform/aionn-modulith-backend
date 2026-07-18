package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ordering.voucher")
public record OrderingVoucherProperties(
        @DefaultValue("no-discount") String provider) {
}
