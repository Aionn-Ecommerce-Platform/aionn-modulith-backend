package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.StockAlertPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.contracts.event.catalog.ProductBackInStockIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");

    @Mock
    private StockAlertPersistencePort alertRepository;
    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private IntegrationEventPublisher integrationEventPublisher;
    @Mock
    private Product product;

    @Test
    void notifiesEveryPendingAlertOnce() {
        when(alertRepository.findPendingBySku("SKU_1")).thenReturn(List.of(
                new StockAlertPersistencePort.PendingAlert(
                        "alert-1", "user-1", "product-1", "SKU_1", NOW),
                new StockAlertPersistencePort.PendingAlert(
                        "alert-2", "user-2", "product-1", "SKU_1", NOW)));
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
        when(product.getName()).thenReturn("Ao thun");

        service().notifyRestocked("SKU_1", NOW);

        ArgumentCaptor<ProductBackInStockIntegrationEvent> captor =
                ArgumentCaptor.forClass(ProductBackInStockIntegrationEvent.class);
        verify(integrationEventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ProductBackInStockIntegrationEvent::userId)
                .containsExactly("user-1", "user-2");
        verify(alertRepository).markNotified("alert-1", NOW);
        verify(alertRepository).markNotified("alert-2", NOW);
    }

    @Test
    void doesNothingWhenNoAlertIsPending() {
        when(alertRepository.findPendingBySku("SKU_1")).thenReturn(List.of());

        service().notifyRestocked("SKU_1", NOW);

        verify(integrationEventPublisher, never()).publish(any());
    }

    private StockAlertService service() {
        return new StockAlertService(alertRepository, productRepository, integrationEventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
