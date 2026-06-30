package com.aionn.sharedkernel.application.port;

import com.aionn.sharedkernel.domain.model.EventEnvelope;

import java.util.Collection;
import java.util.List;

public interface EventPublisher {

    void publish(Collection<EventEnvelope> events);

    default void publish(EventEnvelope event) {
        publish(List.of(event));
    }
}
