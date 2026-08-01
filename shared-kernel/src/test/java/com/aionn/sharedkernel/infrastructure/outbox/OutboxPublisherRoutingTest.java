package com.aionn.sharedkernel.infrastructure.outbox;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.infrastructure.event.SpringEventPublisher;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.SpringIntegrationEventPublisher;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OutboxPublisherRoutingTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void routesDomainEventsToOutboxWhenStoreIsConfigured() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        OutboxEventStore store = mock(OutboxEventStore.class);
        EventEnvelope envelope = new EventEnvelope("evt-1", "Order", "order-1",
                new DomainPayload(OCCURRED_AT), OCCURRED_AT);
        SpringEventPublisher publisher = new SpringEventPublisher(delegate, store);

        publisher.publish(List.of(envelope));
        publisher.publish(List.of());
        publisher.publish((java.util.Collection<EventEnvelope>) null);

        verify(store).append(envelope);
        verify(delegate, never()).publishEvent(envelope);
    }

    @Test
    void routesIntegrationEventsToOutboxWhenStoreIsConfigured() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        OutboxEventStore store = mock(OutboxEventStore.class);
        IntegrationEvent event = new OrderEvent("evt-2", "order-2", OCCURRED_AT);
        SpringIntegrationEventPublisher publisher = new SpringIntegrationEventPublisher(delegate, store);

        publisher.publish(event);
        publisher.publishAll(List.of(event));
        publisher.publishAll(List.of());
        publisher.publishAll(null);

        verify(store, org.mockito.Mockito.times(2)).append(event);
        verify(delegate, never()).publishEvent(event);
    }

    private record DomainPayload(Instant occurredAt) implements DomainEvent { }

    private record OrderEvent(String eventId, String orderId, Instant occurredAt)
            implements IntegrationEvent { }
}
