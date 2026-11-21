package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.infrastructure.persistence.repository.ShipmentItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentItemQueryAdapterTest {

    @Mock
    private ShipmentItemRepository repository;

    @InjectMocks
    private ShipmentItemQueryAdapter adapter;

    @Test
    void mapsDeliveredRows() {
        Instant deliveredAt = Instant.parse("2026-01-02T00:00:00Z");
        when(repository.findDeliveredByOrderId("order-1"))
                .thenReturn(List.<Object[]>of(new Object[] {"sku-1", 2, deliveredAt}));

        var items = adapter.deliveredItems("order-1");

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.skuId()).isEqualTo("sku-1");
            assertThat(item.qty()).isEqualTo(2);
            assertThat(item.deliveredAt()).isEqualTo(deliveredAt);
        });
    }
}
