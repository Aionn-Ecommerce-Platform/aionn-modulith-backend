package com.aionn.sharedkernel.domain.model;

import com.aionn.sharedkernel.util.IdGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot {

    private final List<EventEnvelope> events = new ArrayList<>();

    protected abstract String aggregateId();

    protected String aggregateType() {
        return this.getClass().getSimpleName();
    }

    protected void registerEvent(DomainEvent event) {
        events.add(new EventEnvelope(
                IdGenerator.ulid(),
                aggregateType(),
                aggregateId(),
                event,
                event.occurredAt()));
    }

    public List<EventEnvelope> pullEvents() {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventEnvelope> snapshot = List.copyOf(events);
        events.clear();
        return snapshot;
    }

    public List<EventEnvelope> peekEvents() {
        return Collections.unmodifiableList(events);
    }

    public boolean hasUnpublishedEvents() {
        return !events.isEmpty();
    }
}
