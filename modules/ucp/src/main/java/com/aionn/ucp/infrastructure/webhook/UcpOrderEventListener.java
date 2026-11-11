package com.aionn.ucp.infrastructure.webhook;

import com.aionn.contracts.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderShippedIntegrationEvent;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Correlates order lifecycle events with the originating checkout session and enqueues a durable
 * webhook delivery. The delivery scheduler owns retries so a platform outage cannot lose events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcpOrderEventListener {

    private static final String KEY_STATUS = "status";

    private final UcpCheckoutSessionPort sessionPort;
    private final UcpWebhookDeliveryStore deliveryStore;
    private final Clock clock;

    @EventListener
    public void onOrderPlaced(OrderPlacedIntegrationEvent event) {
        enqueueIfWebhookRegistered(event.eventId(), event.orderId(), event.occurredAt(), "order.placed", Map.of(
                KEY_STATUS, "PLACED",
                "total_amount", event.totalAmount() != null ? event.totalAmount() : 0,
                "currency", event.currency() != null ? event.currency() : ""));
    }

    @EventListener
    public void onOrderApproved(OrderApprovedIntegrationEvent event) {
        enqueueIfWebhookRegistered(event.eventId(), event.orderId(), event.occurredAt(), "order.approved", Map.of(
                KEY_STATUS, "APPROVED",
                "payment_id", event.paymentId() != null ? event.paymentId() : ""));
    }

    @EventListener
    public void onOrderShipped(OrderShippedIntegrationEvent event) {
        enqueueIfWebhookRegistered(event.eventId(), event.orderId(), event.occurredAt(), "order.shipped", Map.of(
                KEY_STATUS, "SHIPPED",
                "shipment_id", event.shipmentId() != null ? event.shipmentId() : ""));
    }

    @EventListener
    public void onOrderCompleted(OrderCompletedIntegrationEvent event) {
        enqueueIfWebhookRegistered(event.eventId(), event.orderId(), event.occurredAt(), "order.completed", Map.of(
                KEY_STATUS, "COMPLETED"));
    }

    @EventListener
    public void onOrderCancelled(OrderCancelledIntegrationEvent event) {
        enqueueIfWebhookRegistered(event.eventId(), event.orderId(), event.occurredAt(), "order.cancelled", Map.of(
                KEY_STATUS, "CANCELLED",
                "reason_code", event.reasonCode() != null ? event.reasonCode() : "",
                "reason", event.reason() != null ? event.reason() : ""));
    }

    private void enqueueIfWebhookRegistered(String eventId, String orderId, Instant occurredAt,
            String eventType, Map<String, Object> data) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }

        Optional<UcpCheckoutSession> sessionOpt = sessionPort.findByOrderId(orderId);
        if (sessionOpt.isEmpty()) {
            log.trace("No checkout session found for order '{}'; skipping UCP webhook dispatch", orderId);
            return;
        }

        UcpCheckoutSession session = sessionOpt.get();
        String webhookUrl = extractWebhookUrl(session);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.trace("Checkout session '{}' has no webhook_url in context; skipping dispatch", session.id());
            return;
        }

        UcpWebhookEvent webhookEvent = new UcpWebhookEvent(
                eventId,
                eventType,
                orderId,
                occurredAt != null ? occurredAt : clock.instant(),
                data);
        deliveryStore.enqueue(webhookUrl, webhookEvent);
    }

    private String extractWebhookUrl(UcpCheckoutSession session) {
        if (session.context() == null) {
            return null;
        }
        Object url = session.context().get("webhook_url");
        return url instanceof String s && !s.isBlank() ? s : null;
    }
}
