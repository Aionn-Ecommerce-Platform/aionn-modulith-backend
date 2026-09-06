package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.port.out.integration.CartIntegrationEventPublisherPort;
import com.aionn.sharedkernel.integration.event.ordering.CartItemAddedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CartIntegrationEventPublisher implements CartIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void publishCartItemAdded(
            String cartId, String userId, String skuId, int quantity, Instant occurredAt) {
        integrationEventPublisher.publish(new CartItemAddedIntegrationEvent(
                IdGenerator.ulid(), userId, cartId, skuId, quantity, occurredAt));
    }
}
