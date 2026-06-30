package com.aionn.sharedkernel.testing;

import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FakeEventPublisher implements EventPublisher {

    private final List<EventEnvelope> envelopes = new ArrayList<>();

    @Override
    public void publish(Collection<EventEnvelope> newEvents) {
        if (newEvents != null) {
            envelopes.addAll(newEvents);
        }
    }

    public List<EventEnvelope> getEnvelopes() {
        return Collections.unmodifiableList(envelopes);
    }

    public List<DomainEvent> getEvents() {
        return envelopes.stream().map(EventEnvelope::payload).toList();
    }

    public <T extends DomainEvent> List<T> eventsOf(Class<T> type) {
        return envelopes.stream()
                .map(EventEnvelope::payload)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public <T extends DomainEvent> T firstOf(Class<T> type) {
        return eventsOf(type).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No event of type " + type.getSimpleName() + " was published"));
    }

    public boolean hasPublished(Class<? extends DomainEvent> type) {
        return envelopes.stream().map(EventEnvelope::payload).anyMatch(type::isInstance);
    }

    public int eventCount() {
        return envelopes.size();
    }

    public boolean isEmpty() {
        return envelopes.isEmpty();
    }

    public void assertPublished(Class<? extends DomainEvent> type) {
        if (!hasPublished(type)) {
            throw new AssertionError(
                    "Expected event of type [%s] to be published, but it wasn't. Published: %s"
                            .formatted(type.getSimpleName(), eventNames()));
        }
    }

    public void assertNotPublished(Class<? extends DomainEvent> type) {
        if (hasPublished(type)) {
            throw new AssertionError("Expected no event of type [%s], but it was published."
                    .formatted(type.getSimpleName()));
        }
    }

    public void assertPublishedCount(int expectedCount) {
        if (envelopes.size() != expectedCount) {
            throw new AssertionError("Expected %d event(s), but got %d. Events: %s"
                    .formatted(expectedCount, envelopes.size(), eventNames()));
        }
    }

    public void assertNoEventsPublished() {
        if (!envelopes.isEmpty()) {
            throw new AssertionError("Expected no events, but %d were published: %s"
                    .formatted(envelopes.size(), eventNames()));
        }
    }

    public void reset() {
        envelopes.clear();
    }

    private List<String> eventNames() {
        return envelopes.stream()
                .map(e -> e.payload().getClass().getSimpleName())
                .collect(Collectors.toList());
    }
}
