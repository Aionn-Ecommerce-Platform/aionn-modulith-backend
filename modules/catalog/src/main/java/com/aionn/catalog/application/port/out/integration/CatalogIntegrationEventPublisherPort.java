package com.aionn.catalog.application.port.out.integration;

import java.time.Instant;
import java.util.List;

public interface CatalogIntegrationEventPublisherPort {

        void publishProductViewed(
                        String productId,
                        String userId,
                        String brandId,
                        List<String> categoryIds,
                        Instant occurredAt);

        void publishSearchPerformed(
                        String userId,
                        String keyword,
                        List<String> categoryIds,
                        String brandId,
                        Instant occurredAt);
}
