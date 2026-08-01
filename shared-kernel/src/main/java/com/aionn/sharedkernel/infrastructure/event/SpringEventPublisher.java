package com.aionn.sharedkernel.infrastructure.event;

import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.infrastructure.outbox.OutboxEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SpringEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventStore outboxEventStore;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this(applicationEventPublisher, null);
    }

    @Autowired
    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher,
            OutboxEventStore outboxEventStore) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.outboxEventStore = outboxEventStore;
    }

    @Override
    public void publish(Collection<EventEnvelope> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (EventEnvelope envelope : events) {
            if (log.isDebugEnabled()) {
                log.debug("Publishing domain event: {} [{}]", envelope.eventType(), envelope.eventId());
            }
            if (outboxEventStore != null) {
                outboxEventStore.append(envelope);
            } else {
                applicationEventPublisher.publishEvent(envelope);
            }
        }
    }
}
