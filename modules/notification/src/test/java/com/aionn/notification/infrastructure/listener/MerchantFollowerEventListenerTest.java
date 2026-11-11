package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.catalog.MerchantFollowerNotificationIntegrationEvent;
import com.aionn.contracts.port.catalog.MerchantFollowerQueryPort;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantFollowerEventListenerTest {

    @Mock
    private MerchantFollowerQueryPort followerQueryPort;
    @Mock
    private NotificationDeliveryOrchestrator deliveryOrchestrator;

    @Test
    void queuesOneNotificationPerFollowerAndHandsThemToTheAsyncExecutor() {
        when(followerQueryPort.findFollowerIds("MER_1")).thenReturn(List.of("USER_1", "USER_2"));
        MerchantFollowerEventListener listener = new MerchantFollowerEventListener(
                followerQueryPort, deliveryOrchestrator);

        listener.on(new MerchantFollowerNotificationIntegrationEvent(
                "evt_1", "MER_1", "PRODUCT_PUBLISHED", "Sản phẩm mới", "Áo thun",
                "/products/P1", Instant.now()));

        verify(deliveryOrchestrator, times(2)).enqueueByEvent(any());
        verify(deliveryOrchestrator, times(1)).deliverQueuedAsync(any());
        verify(deliveryOrchestrator, never()).sendByEvent(any());
    }
}
