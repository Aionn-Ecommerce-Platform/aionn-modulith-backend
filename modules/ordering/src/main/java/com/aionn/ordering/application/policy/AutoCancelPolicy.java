package com.aionn.ordering.application.policy;

import java.time.Duration;
import java.time.Instant;

public interface AutoCancelPolicy {

    Duration timeout();

    int batchSize();

    Instant cutoff(Instant now);
}
