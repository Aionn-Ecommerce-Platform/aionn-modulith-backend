package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ordering.reservation")
public record OrderingReservationProperties(
        @DefaultValue("900") int ttlSeconds) {
}
