package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.application.port.out.integration.CatalogIntegrationEventPublisherPort;
import com.aionn.sharedkernel.integration.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogOutboundEventPublisher implements CatalogIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void publishProductViewed(
            String productId,
            String userId,
            String brandId,
            List<String> categoryIds,
            Instant occurredAt) {
        integrationEventPublisher.publish(new ProductViewedIntegrationEvent(
                IdGenerator.ulid(), productId, userId, brandId, categoryIds, occurredAt));
    }
}
