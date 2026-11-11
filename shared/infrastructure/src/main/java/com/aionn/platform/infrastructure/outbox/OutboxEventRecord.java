package com.aionn.platform.infrastructure.outbox;

import java.time.Instant;

record OutboxEventRecord(
        String eventId,
        String eventKind,
        String eventType,
        String payloadType,
        String payload,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int attempts) {
}
