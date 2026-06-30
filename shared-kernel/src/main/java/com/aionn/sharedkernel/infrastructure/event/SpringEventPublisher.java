package com.aionn.sharedkernel.infrastructure.event;

import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SpringEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
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
            applicationEventPublisher.publishEvent(envelope);
        }
    }
}
