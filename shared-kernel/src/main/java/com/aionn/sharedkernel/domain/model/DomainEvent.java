package com.aionn.sharedkernel.domain.model;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();

    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
