package com.aionn.sharedkernel.integration.publisher;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import com.aionn.sharedkernel.infrastructure.outbox.OutboxEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

@Slf4j
@Component
public class SpringIntegrationEventPublisher implements IntegrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventStore outboxEventStore;

    public SpringIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this(applicationEventPublisher, (OutboxEventStore) null);
    }

    @Autowired
    public SpringIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher,
            ObjectProvider<OutboxEventStore> outboxEventStoreProvider) {
        this(applicationEventPublisher, outboxEventStoreProvider.getIfAvailable());
    }

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
            publishAfterCommit(event);
        }

        log.trace("Integration event published successfully: {}", event.eventType());
    }

    private void publishAfterCommit(IntegrationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applicationEventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationEventPublisher.publishEvent(event);
            }
        });
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
