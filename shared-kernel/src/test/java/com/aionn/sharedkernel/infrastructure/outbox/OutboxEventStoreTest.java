package com.aionn.sharedkernel.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxEventStoreTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void appendsDomainEventWithAggregateOrderingKey() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        DomainPayload payload = new DomainPayload(OCCURRED_AT);
        when(mapper.writeValueAsString(payload)).thenReturn("{\"value\":1}");

        new OutboxEventStore(jdbc, mapper).append(
                new EventEnvelope("evt-1", "Order", "order-1", payload, OCCURRED_AT));

        verify(jdbc).update(anyString(), any(Object[].class));
        verify(mapper).writeValueAsString(payload);
    }

    @Test
    void derivesIntegrationOrderingKeyFromFirstAggregateIdComponent() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        OrderEvent event = new OrderEvent("evt-2", "order-2", OCCURRED_AT);
        when(mapper.writeValueAsString(event)).thenReturn("{}");

        new OutboxEventStore(jdbc, mapper).append(event);

        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    void fallsBackToEventIdForNonRecordIntegrationEvent() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        IntegrationEvent event = mock(IntegrationEvent.class);
        when(event.eventId()).thenReturn("evt-3");
        when(event.eventType()).thenReturn("sample");
        when(event.occurredAt()).thenReturn(OCCURRED_AT);
        when(mapper.writeValueAsString(event)).thenReturn("{}");

        new OutboxEventStore(jdbc, mapper).append(event);

        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsPayloadThatCannotBeSerialized() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        DomainPayload payload = new DomainPayload(OCCURRED_AT);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("broken") { });
        OutboxEventStore store = new OutboxEventStore(mock(JdbcTemplate.class), mapper);

        assertThrows(IllegalArgumentException.class, () -> store.append(
                new EventEnvelope("evt-4", "Order", "order-4", payload, OCCURRED_AT)));
    }

    private record DomainPayload(Instant occurredAt) implements DomainEvent { }

    private record OrderEvent(String eventId, String orderId, Instant occurredAt)
            implements IntegrationEvent { }
}
