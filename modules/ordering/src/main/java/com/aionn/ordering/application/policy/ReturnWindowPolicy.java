package com.aionn.ordering.application.policy;

import java.time.Duration;
import java.time.Instant;

public interface ReturnWindowPolicy {

    Duration windowDuration();

    boolean isWithinWindow(Instant completedAt, Instant now);
}
