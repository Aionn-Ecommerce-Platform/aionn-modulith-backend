package com.aionn.sharedkernel.integration.publisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class IntegrationPublisherSupportTest {

    @Test
    void springIntegrationEventPublisherPublishesSingleAndBatchEvents() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        SpringIntegrationEventPublisher publisher = new SpringIntegrationEventPublisher(delegate);
        IntegrationEvent first = new SampleIntegrationEvent("catalog.product-updated");
        IntegrationEvent second = new SampleIntegrationEvent("inventory.stock-reserved");

        publisher.publish(first);
        publisher.publishAll(List.of(first, second));
        publisher.publishAll(List.of());
        publisher.publishAll(null);

        verify(delegate, times(2)).publishEvent(first);
        verify(delegate).publishEvent(second);
    }

    @Test
    void integrationEventPublisherContractCanBeImplemented() {
        IntegrationEventPublisher publisher = new IntegrationEventPublisher() {
            @Override
            public void publish(IntegrationEvent event) {
                // No-op test double; contract instantiation is the behavior under test.
            }

            @Override
            public void publishAll(java.util.Collection<IntegrationEvent> events) {
                // No-op test double; contract instantiation is the behavior under test.
            }
        };

        assertDoesNotThrow(() -> publisher.publish(new SampleIntegrationEvent("test.event")));
        assertDoesNotThrow(() -> publisher.publishAll(List.of(new SampleIntegrationEvent("test.batch"))));
    }

    record SampleIntegrationEvent(String eventType) implements IntegrationEvent {
        @Override
        public String eventId() {
            return "evt-1";
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-07-01T00:00:00Z");
        }
    }
}
