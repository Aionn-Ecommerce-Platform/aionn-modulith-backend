package com.aionn.sharedkernel.infrastructure.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OutboxDispatcherTest {

    @Test
    void dispatchesAndRecordsInboxBeforeCompletingOutbox() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SampleEvent event = new SampleEvent("evt-1", "order-1", Instant.parse("2026-01-01T00:00:00Z"));
        OutboxEventRecord record = new OutboxEventRecord(event.eventId(), "INTEGRATION",
                event.eventType(), SampleEvent.class.getName(), mapper.writeValueAsString(event),
                event.eventType(), event.orderId(), event.occurredAt(), 1);
        when(repository.claim(any(Integer.class), any(String.class), any())).thenReturn(List.of(record));

        new OutboxDispatcher(repository, mapper, publisher, 10, 3).dispatch();

        verify(publisher).publishEvent(event);
        verify(repository).markProcessed("spring-event-bus:" + event.eventType(), event.eventId());
        verify(repository).markPublished(event.eventId());
    }

    @Test
    void skipsAlreadyProcessedEventAndCompletesOutbox() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SampleEvent event = new SampleEvent("evt-2", "order-2", Instant.parse("2026-01-01T00:00:00Z"));
        OutboxEventRecord record = new OutboxEventRecord(event.eventId(), "INTEGRATION",
                event.eventType(), SampleEvent.class.getName(), mapper.writeValueAsString(event),
                event.eventType(), event.orderId(), event.occurredAt(), 2);
        String consumer = "spring-event-bus:" + event.eventType();
        when(repository.claim(any(Integer.class), any(String.class), any())).thenReturn(List.of(record));
        when(repository.wasProcessed(consumer, event.eventId())).thenReturn(true);

        new OutboxDispatcher(repository, mapper, publisher, 10, 3).dispatch();

        verify(publisher, never()).publishEvent(any(Object.class));
        verify(repository).markPublished(event.eventId());
    }

    @Test
    void retriesFailedDispatchAndDeadLettersAtLimit() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OutboxEventRecord record = new OutboxEventRecord("evt-3", "INTEGRATION", "broken",
                "missing.Event", "{}", "broken", "id", Instant.now(), 3);
        when(repository.claim(any(Integer.class), any(String.class), any())).thenReturn(List.of(record));

        new OutboxDispatcher(repository, mapper, publisher, 10, 3).dispatch();

        verify(repository).markFailed(eq("evt-3"), any(), any(), eq(true));
        verify(repository, never()).markPublished("evt-3");
    }

    @Test
    void dispatchesDomainEnvelopeAndRetriesBeforeAttemptLimit() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DomainPayload payload = new DomainPayload(Instant.parse("2026-01-01T00:00:00Z"));
        OutboxEventRecord record = new OutboxEventRecord("evt-4", "DOMAIN", payload.eventType(),
                DomainPayload.class.getName(), mapper.writeValueAsString(payload), "Order", "order-4",
                payload.occurredAt(), 1);
        when(repository.claim(any(Integer.class), any(String.class), any())).thenReturn(List.of(record));

        new OutboxDispatcher(repository, mapper, publisher, 10, 3).dispatch();

        verify(publisher).publishEvent(any(com.aionn.sharedkernel.domain.model.EventEnvelope.class));

        org.mockito.Mockito.doThrow(new IllegalStateException("temporary"))
                .when(publisher).publishEvent(any(Object.class));
        new OutboxDispatcher(repository, mapper, publisher, 10, 3).dispatch();
        verify(repository).markFailed(eq("evt-4"), eq("temporary"), any(), eq(false));
    }

    private record SampleEvent(String eventId, String orderId, Instant occurredAt) implements IntegrationEvent {
    }

    private record DomainPayload(Instant occurredAt) implements DomainEvent {
    }
}
