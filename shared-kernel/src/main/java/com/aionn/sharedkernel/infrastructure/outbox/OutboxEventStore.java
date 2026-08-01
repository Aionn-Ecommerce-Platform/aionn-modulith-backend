package com.aionn.sharedkernel.infrastructure.outbox;

import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventStore {

    private static final String INSERT_SQL = """
            INSERT INTO outbox_events
                (event_id, event_kind, event_type, payload_type, payload, aggregate_type,
                 aggregate_id, ordering_key, occurred_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void append(EventEnvelope envelope) {
        append(envelope.eventId(), "DOMAIN", envelope.eventType(), envelope.payload(),
                envelope.aggregateType(), envelope.aggregateId(), envelope.occurredAt());
    }

    public void append(IntegrationEvent event) {
        String aggregateId = resolveAggregateId(event);
        append(event.eventId(), "INTEGRATION", event.eventType(), event,
                event.eventType(), aggregateId, event.occurredAt());
    }

    private void append(String eventId, String kind, String eventType, Object payload,
            String aggregateType, String aggregateId, Instant occurredAt) {
        try {
            jdbcTemplate.update(INSERT_SQL, eventId, kind, eventType, payload.getClass().getName(),
                    objectMapper.writeValueAsString(payload), aggregateType, aggregateId,
                    aggregateType + ":" + aggregateId, occurredAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize event " + eventType, exception);
        }
    }

    private static String resolveAggregateId(IntegrationEvent event) {
        if (!event.getClass().isRecord()) {
            return event.eventId();
        }
        for (RecordComponent component : event.getClass().getRecordComponents()) {
            if (!component.getName().equals("eventId") && component.getName().endsWith("Id")) {
                try {
                    Object value = component.getAccessor().invoke(event);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Unable to resolve event ordering key", exception);
                }
            }
        }
        return event.eventId();
    }
}
