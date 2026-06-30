package com.aionn.sharedkernel.integration.mapper;

public interface IntegrationEventMapper<D, I> {

    I toIntegrationEvent(D domainEvent);
}
