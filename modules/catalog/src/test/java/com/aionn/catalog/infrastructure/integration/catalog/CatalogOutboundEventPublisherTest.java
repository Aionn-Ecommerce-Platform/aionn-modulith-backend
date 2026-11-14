package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.contracts.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.contracts.event.catalog.SearchPerformedIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogOutboundEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @InjectMocks
    private CatalogOutboundEventPublisher publisher;

    @Test
    void publishesProductViewedWithTheAggregateContext() {
        publisher.publishProductViewed("product-1", "user-1", "brand-1",
                List.of("category-1"), NOW);

        ArgumentCaptor<ProductViewedIntegrationEvent> captor =
                ArgumentCaptor.forClass(ProductViewedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        ProductViewedIntegrationEvent event = captor.getValue();
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.productId()).isEqualTo("product-1");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.brandId()).isEqualTo("brand-1");
        assertThat(event.categoryIds()).containsExactly("category-1");
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void publishesSearchPerformedWithKeywordAndFacets() {
        publisher.publishSearchPerformed("user-1", "widget", List.of("category-1"),
                "brand-1", NOW);

        ArgumentCaptor<SearchPerformedIntegrationEvent> captor =
                ArgumentCaptor.forClass(SearchPerformedIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        SearchPerformedIntegrationEvent event = captor.getValue();
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.keyword()).isEqualTo("widget");
        assertThat(event.categoryIds()).containsExactly("category-1");
        assertThat(event.brandId()).isEqualTo("brand-1");
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }
}
