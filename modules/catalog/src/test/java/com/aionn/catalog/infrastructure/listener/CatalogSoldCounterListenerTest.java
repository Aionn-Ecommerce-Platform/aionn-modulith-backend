package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.infrastructure.search.ProductSearchIndexUpdater;
import com.aionn.contracts.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.contracts.port.ordering.OrderSnapshotQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSoldCounterListenerTest {

    @Mock
    private OrderSnapshotQueryPort orderSnapshotQueryPort;
    @Mock
    private ProductPersistencePort productPersistencePort;
    @Mock
    private ProductSoldCounterPersistencePort productSoldCounterPersistencePort;
    @Mock
    private ProductSearchIndexUpdater searchIndexUpdater;

    @Test
    void incrementsSoldCountersForCompletedOrderLines() {
        OrderSnapshotQueryPort.OrderSnapshot snapshot = new OrderSnapshotQueryPort.OrderSnapshot(
                "ord_1", "user_1", "merchant_1", "VND", "COMPLETED", 0, 0, 0,
                List.of(
                        new OrderSnapshotQueryPort.OrderSnapshot.Line("SKU_A", 2, 0, 0),
                        new OrderSnapshotQueryPort.OrderSnapshot.Line("SKU_B", 3, 0, 0)));
        Product product = org.mockito.Mockito.mock(Product.class);
        when(product.getProductId()).thenReturn("P1");
        when(product.variants()).thenReturn(List.of(
                new ProductVariant("SKU_A", Map.of(), null),
                new ProductVariant("SKU_B", Map.of(), null)));
        when(orderSnapshotQueryPort.findOrderById("ord_1")).thenReturn(Optional.of(snapshot));
        when(productPersistencePort.findAllBySkuIds(any())).thenReturn(List.of(product));

        listener().onOrderCompleted(new OrderCompletedIntegrationEvent("evt_1", "ord_1", Instant.now()));

        verify(productSoldCounterPersistencePort).incrementSoldCount("P1", 5L);
        verify(searchIndexUpdater).reindexProduct("P1");
    }

    @Test
    void ignoresMissingOrderSnapshot() {
        when(orderSnapshotQueryPort.findOrderById("ord_missing")).thenReturn(Optional.empty());

        listener().onOrderCompleted(new OrderCompletedIntegrationEvent("evt_1", "ord_missing", Instant.now()));

        verify(productSoldCounterPersistencePort, never()).incrementSoldCount(any(), anyLong());
    }

    private CatalogSoldCounterListener listener() {
        return new CatalogSoldCounterListener(orderSnapshotQueryPort, productPersistencePort,
                productSoldCounterPersistencePort, searchIndexUpdater);
    }
}
