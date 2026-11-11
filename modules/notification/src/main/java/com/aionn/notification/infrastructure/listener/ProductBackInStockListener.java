package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.catalog.ProductBackInStockIntegrationEvent;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductBackInStockListener {

    private static final String EVENT_BACK_IN_STOCK = "catalog.product-back-in-stock";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @EventListener
    public void on(ProductBackInStockIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("productId", event.productId());
        context.put("skuId", event.skuId());
        context.put("productName", event.productName() == null ? "" : event.productName());
        context.put("targetUrl", "/products/" + event.productId());

        List<Notification> queued = deliveryOrchestrator.enqueueByEvent(new NotificationCommands.SendByEvent(
                event.userId(),
                EVENT_BACK_IN_STOCK,
                NotificationCategory.PROMOTION,
                List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
                null,
                null,
                context));
        deliveryOrchestrator.deliverQueuedAsync(queued);
    }
}
