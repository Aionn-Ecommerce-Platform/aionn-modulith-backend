package com.aionn.promotion.application.policy;

import java.time.Duration;
import java.time.Instant;

public interface VoucherReservationPolicy {

    Duration ttl();

    Instant expiresFrom(Instant now);
}
