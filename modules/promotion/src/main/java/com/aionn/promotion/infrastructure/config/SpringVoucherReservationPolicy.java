package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.application.policy.VoucherReservationPolicy;
import com.aionn.promotion.infrastructure.config.properties.PromotionVoucherProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SpringVoucherReservationPolicy implements VoucherReservationPolicy {

    private final PromotionVoucherProperties properties;

    @Override
    public Duration ttl() {
        int seconds = properties.reservationTtlSeconds();
        return Duration.ofSeconds(seconds <= 0 ? 900 : seconds);
    }

    @Override
    public Instant expiresFrom(Instant now) {
        return now.plus(ttl());
    }
}
