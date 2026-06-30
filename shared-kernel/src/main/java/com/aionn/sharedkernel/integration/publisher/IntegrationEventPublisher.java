package com.aionn.sharedkernel.integration.publisher;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.util.Collection;

public interface IntegrationEventPublisher {

    void publish(IntegrationEvent event);

    void publishAll(Collection<IntegrationEvent> events);
}
