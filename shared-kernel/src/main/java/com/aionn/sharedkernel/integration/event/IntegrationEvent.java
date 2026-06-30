package com.aionn.sharedkernel.integration.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface IntegrationEvent {

    String eventId();

    Instant occurredAt();

    default String eventType() {
        return this.getClass().getName();
    }

    static String requireEventId(String eventId) {
        String normalized = Objects.requireNonNull(eventId, "eventId must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return normalized;
    }

    static Instant defaultOccurredAt(Instant occurredAt) {
        return occurredAt != null ? occurredAt : Instant.now();
    }

    static <T> List<T> freezeList(List<T> items, String fieldName) {
        Objects.requireNonNull(items, fieldName + " must not be null");
        return List.copyOf(items);
    }
}
