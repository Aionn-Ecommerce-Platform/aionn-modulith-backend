package com.aionn.sharedkernel.integration.publisher;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import com.aionn.sharedkernel.infrastructure.outbox.OutboxEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
public class SpringIntegrationEventPublisher implements IntegrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventStore outboxEventStore;

    public SpringIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this(applicationEventPublisher, null);
    }

    @Autowired
    public SpringIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher,
            OutboxEventStore outboxEventStore) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.outboxEventStore = outboxEventStore;
    }

    @Override
    public void publish(IntegrationEvent event) {
        log.debug("Publishing integration event: {} [eventId={}, occurredAt={}]",
                event.eventType(), event.eventId(), event.occurredAt());

        if (outboxEventStore != null) {
            outboxEventStore.append(event);
        } else {
            applicationEventPublisher.publishEvent(event);
        }

        log.trace("Integration event published successfully: {}", event.eventType());
    }

    @Override
    public void publishAll(Collection<IntegrationEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        log.debug("Publishing {} integration events in batch", events.size());

        for (IntegrationEvent event : events) {
            publish(event);
        }

        log.trace("Batch of {} integration events published successfully", events.size());
    }
}
