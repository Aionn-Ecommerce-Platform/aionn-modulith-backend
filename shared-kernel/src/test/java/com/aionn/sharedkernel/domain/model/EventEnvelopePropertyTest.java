package com.aionn.sharedkernel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class EventEnvelopePropertyTest {

    private static final String VALID_EVENT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String VALID_AGG_TYPE = "TestAggregate";
    private static final String VALID_AGG_ID = "01BX5ZZKBKACTAV9WEVGEMMVRZ";

    private static DomainEvent validPayload(int seq) {
        return new TestEvent(seq, Instant.ofEpochMilli(seq));
    }

    @Property(tries = 100)
    void property26_rejectsNullFields(
            @ForAll("nullableFieldIndex") int nullField,
            @ForAll @IntRange(min = 0, max = 1_000_000) int seq) {
        String eventId = nullField == 0 ? null : VALID_EVENT_ID;
        String aggregateType = nullField == 1 ? null : VALID_AGG_TYPE;
        String aggregateId = nullField == 2 ? null : VALID_AGG_ID;
        DomainEvent payload = nullField == 3 ? null : validPayload(seq);
        Instant occurredAt = nullField == 4 ? null : Instant.ofEpochMilli(seq);

        assertThrows(NullPointerException.class,
                () -> new EventEnvelope(eventId, aggregateType, aggregateId, payload, occurredAt),
                () -> "EventEnvelope must reject null field index " + nullField);
    }

    @Property(tries = 100)
    void property26_acceptsAllNonNullFields(
            @ForAll @IntRange(min = 0, max = 1_000_000) int seq) {
        DomainEvent payload = validPayload(seq);
        Instant occurredAt = Instant.ofEpochMilli(seq);

        EventEnvelope envelope = new EventEnvelope(
                VALID_EVENT_ID, VALID_AGG_TYPE, VALID_AGG_ID, payload, occurredAt);

        assertNotNull(envelope);
        assertEquals(VALID_EVENT_ID, envelope.eventId());
        assertEquals(VALID_AGG_TYPE, envelope.aggregateType());
        assertEquals(VALID_AGG_ID, envelope.aggregateId());
        assertSame(payload, envelope.payload());
        assertEquals(occurredAt, envelope.occurredAt());
        assertEquals(payload.eventType(), envelope.eventType());
    }

    @Provide
    Arbitrary<Integer> nullableFieldIndex() {
        return Arbitraries.integers().between(0, 4);
    }
}
