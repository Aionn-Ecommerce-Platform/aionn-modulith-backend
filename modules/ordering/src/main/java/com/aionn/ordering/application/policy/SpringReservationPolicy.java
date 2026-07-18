package com.aionn.ordering.application.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingReservationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringReservationPolicy implements ReservationPolicy {

    private final OrderingReservationProperties properties;

    @Override
    public int ttlSeconds() {
        return Math.max(60, properties.ttlSeconds());
    }
}
