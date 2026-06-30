package com.aionn.sharedkernel.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EventEnvelope(
        String eventId,
        String aggregateType,
        String aggregateId,
        DomainEvent payload,
        Instant occurredAt) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public String eventType() {
        return payload.eventType();
    }
}
