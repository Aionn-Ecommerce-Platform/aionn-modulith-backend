package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.catalog.MerchantFollowerNotificationIntegrationEvent;
import com.aionn.contracts.port.catalog.MerchantFollowerQueryPort;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MerchantFollowerEventListener {

    private static final String EVENT_FOLLOWER_UPDATE = "catalog.merchant-follower-update";

    /**
     * A popular shop can have thousands of followers. The outbox thread only writes the rows; they are
     * handed to the delivery executor in chunks so no single task monopolises a worker.
     */
    private static final int DELIVERY_BATCH_SIZE = 100;

    private final MerchantFollowerQueryPort followerQueryPort;
    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @EventListener
    public void on(MerchantFollowerNotificationIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("merchantId", event.merchantId());
        context.put("notificationType", event.notificationType());
        context.put("title", event.title());
        context.put("message", event.message());
        if (event.targetUrl() != null) {
            context.put("targetUrl", event.targetUrl());
        }

        List<Notification> batch = new ArrayList<>(DELIVERY_BATCH_SIZE);
        for (String userId : followerQueryPort.findFollowerIds(event.merchantId())) {
            batch.addAll(deliveryOrchestrator.enqueueByEvent(new NotificationCommands.SendByEvent(
                    userId,
                    EVENT_FOLLOWER_UPDATE,
                    NotificationCategory.PROMOTION,
                    List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
                    null,
                    null,
                    context)));
            if (batch.size() >= DELIVERY_BATCH_SIZE) {
                deliveryOrchestrator.deliverQueuedAsync(List.copyOf(batch));
                batch.clear();
            }
        }
        deliveryOrchestrator.deliverQueuedAsync(List.copyOf(batch));
    }
}
