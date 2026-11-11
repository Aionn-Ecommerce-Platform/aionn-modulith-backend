package com.aionn.contracts.mapper;

public interface IntegrationEventMapper<D, I> {

    I toIntegrationEvent(D domainEvent);
}
