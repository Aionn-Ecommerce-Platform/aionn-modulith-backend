package com.aionn.contracts.publisher;

import com.aionn.contracts.event.IntegrationEvent;

import java.util.Collection;

public interface IntegrationEventPublisher {

    void publish(IntegrationEvent event);

    void publishAll(Collection<IntegrationEvent> events);
}
