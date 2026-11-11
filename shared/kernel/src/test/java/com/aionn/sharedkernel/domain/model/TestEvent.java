package com.aionn.sharedkernel.domain.model;

import java.time.Instant;

public record TestEvent(int sequence, Instant occurredAt) implements DomainEvent {
}
