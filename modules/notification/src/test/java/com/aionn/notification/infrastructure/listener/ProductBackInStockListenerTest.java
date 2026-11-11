package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.catalog.ProductBackInStockIntegrationEvent;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductBackInStockListenerTest {

    @Mock
    private NotificationDeliveryOrchestrator deliveryOrchestrator;

    @Test
    void queuesOneNotificationForTheSubscriber() {
        new ProductBackInStockListener(deliveryOrchestrator)
                .on(new ProductBackInStockIntegrationEvent(
                        "evt-1", "user-1", "product-1", "SKU_1", "Ao thun", Instant.now()));

        verify(deliveryOrchestrator).enqueueByEvent(any());
        verify(deliveryOrchestrator).deliverQueuedAsync(any());
        verify(deliveryOrchestrator, never()).sendByEvent(any());
    }
}
