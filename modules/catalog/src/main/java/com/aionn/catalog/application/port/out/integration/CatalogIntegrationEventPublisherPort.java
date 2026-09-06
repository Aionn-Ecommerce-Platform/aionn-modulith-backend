package com.aionn.catalog.application.port.out.integration;

import java.time.Instant;
import java.util.List;

/**
 * Publishes catalog facts other modules subscribe to. The implementation lives in infrastructure and
 * writes through the transactional outbox, so the event and the mutation that caused it commit
 * together.
 */
public interface CatalogIntegrationEventPublisherPort {

    /**
     * Announces that a user opened a product page. Brand and categories travel with the event so
     * behavioural consumers do not have to call back into catalog for every view - views are the
     * highest-volume signal in the system.
     */
    void publishProductViewed(
            String productId,
            String userId,
            String brandId,
            List<String> categoryIds,
            Instant occurredAt);
}
