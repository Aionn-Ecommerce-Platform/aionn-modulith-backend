package com.aionn.sharedkernel.integration.event;

import java.time.Instant;

public interface IntegrationEvent {

    String eventId();

    Instant occurredAt();

    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
