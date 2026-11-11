package com.aionn.sharedkernel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class AggregateRootPropertyTest {

    private static List<TestEvent> recordEvents(TestAggregate aggregate, int n) {
        List<TestEvent> recorded = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            TestEvent event = new TestEvent(i, Instant.ofEpochMilli(i));
            aggregate.recordEvent(event);
            recorded.add(event);
        }
        return recorded;
    }

    @Property(tries = 100)
    void property23_peekPreservesOrderAndIsNonDestructive(
            @ForAll @IntRange(min = 0, max = 1000) int n) {
        TestAggregate aggregate = new TestAggregate();
        List<TestEvent> recorded = recordEvents(aggregate, n);

        assertEquals(n > 0, aggregate.hasUnpublishedEvents());

        for (int call = 0; call < 3; call++) {
            List<EventEnvelope> peeked = aggregate.peekEvents();
            assertEquals(n, peeked.size(),
                    () -> "peekEvents() must return N envelopes on every call");
            for (int i = 0; i < n; i++) {
                assertSame(recorded.get(i), peeked.get(i).payload(),
                        "peekEvents() must preserve record order");
            }
        }

        assertEquals(n > 0, aggregate.hasUnpublishedEvents());
    }

    @Property(tries = 100)
    void property24_pullReturnsInOrderThenIdempotentlyEmpty(
            @ForAll @IntRange(min = 1, max = 1000) int n) {
        TestAggregate aggregate = new TestAggregate();
        List<TestEvent> recorded = recordEvents(aggregate, n);

        List<EventEnvelope> pulled = aggregate.pullEvents();
        assertEquals(n, pulled.size());
        for (int i = 0; i < n; i++) {
            assertSame(recorded.get(i), pulled.get(i).payload(),
                    "pullEvents() must preserve record order");
        }

        assertFalse(aggregate.hasUnpublishedEvents());

        assertTrue(aggregate.pullEvents().isEmpty(),
                "a second pullEvents() must return an empty list");
        assertFalse(aggregate.hasUnpublishedEvents());
    }

    @Property(tries = 100)
    void property25_peekReturnsImmutableView(
            @ForAll @IntRange(min = 1, max = 1000) int n) {
        TestAggregate aggregate = new TestAggregate();
        recordEvents(aggregate, n);

        List<EventEnvelope> view = aggregate.peekEvents();
        EventEnvelope sample = view.get(0);

        assertThrows(UnsupportedOperationException.class, () -> view.add(sample));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(0));
        assertThrows(UnsupportedOperationException.class, view::clear);

        assertEquals(n, aggregate.peekEvents().size());
        assertTrue(aggregate.hasUnpublishedEvents());
    }
}
