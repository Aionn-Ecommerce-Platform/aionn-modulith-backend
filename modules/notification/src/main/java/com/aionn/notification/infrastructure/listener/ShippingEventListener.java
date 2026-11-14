package com.aionn.notification.infrastructure.listener;

import com.aionn.contracts.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentReturnedIntegrationEvent;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventListener {

    private static final String EVENT_DELIVERY_FAILED = "shipping.delivery-failed";
    private static final String EVENT_RETURNED = "shipping.returned";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final OrderQueryPort orderQueryPort;
    private final MerchantQueryPort merchantQueryPort;

    @EventListener
    public void on(ShipmentDeliveryFailedIntegrationEvent event) {
        String userId = event.userId();
        if (userId == null || userId.isBlank()) {
            userId = orderQueryPort.findOrderSummary(event.orderId())
                    .map(OrderQueryPort.OrderSummary::userId)
                    .orElse(null);
        }
        if (userId == null || userId.isBlank()) {
            log.warn("ShipmentDeliveryFailed: cannot resolve recipient for order {}; skipping notification",
                    event.orderId());
            return;
        }

        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("orderId", event.orderId());
        context.put("shipmentId", event.shipmentId());
        context.put("reason", event.reason() == null ? "" : event.reason());
        context.put("attemptCount", String.valueOf(event.attemptCount()));

        List<Notification> queued = deliveryOrchestrator.enqueueByEvent(new NotificationCommands.SendByEvent(
                userId,
                EVENT_DELIVERY_FAILED,
                NotificationCategory.SHIPPING,
                List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
                null,
                null,
                context));
        deliveryOrchestrator.deliverQueuedAsync(queued);
    }

    @EventListener
    public void on(ShipmentReturnedIntegrationEvent event) {
        String ownerId = event.merchantId() == null ? null
                : merchantQueryPort.findOwnerIdByMerchantId(event.merchantId()).orElse(null);
        if (ownerId == null) {
            log.warn("ShipmentReturned: cannot resolve merchant owner for order {}; skipping notification",
                    event.orderId());
            return;
        }

        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("orderId", event.orderId());
        context.put("shipmentId", event.shipmentId());
        context.put("reason", event.reason() == null ? "" : event.reason());

        List<Notification> queued = deliveryOrchestrator.enqueueByEvent(new NotificationCommands.SendByEvent(
                ownerId,
                EVENT_RETURNED,
                NotificationCategory.SHIPPING,
                List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
                null,
                null,
                context));
        deliveryOrchestrator.deliverQueuedAsync(queued);
    }
}
