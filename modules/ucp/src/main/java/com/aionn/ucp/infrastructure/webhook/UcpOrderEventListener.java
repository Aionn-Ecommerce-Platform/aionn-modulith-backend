package com.aionn.ucp.infrastructure.webhook;

import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Event listener for order lifecycle events emitted by ordering module.
 * Correlates order with originating checkout session and dispatches outbound
 * webhooks to platform.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcpOrderEventListener {

    private final UcpCheckoutSessionPort sessionPort;
    private final UcpWebhookDispatcherPort webhookDispatcher;
    private final Clock clock;

    @EventListener
    public void onOrderPlaced(OrderPlacedIntegrationEvent event) {
        dispatchIfWebhookRegistered(event.orderId(), "order.placed", Map.of(
                "status", "PLACED",
                "total_amount", event.totalAmount() != null ? event.totalAmount() : 0,
                "currency", event.currency() != null ? event.currency() : ""));
    }

    @EventListener
    public void onOrderApproved(OrderApprovedIntegrationEvent event) {
        dispatchIfWebhookRegistered(event.orderId(), "order.approved", Map.of(
                "status", "APPROVED",
                "payment_id", event.paymentId() != null ? event.paymentId() : ""));
    }

    @EventListener
    public void onOrderShipped(OrderShippedIntegrationEvent event) {
        dispatchIfWebhookRegistered(event.orderId(), "order.shipped", Map.of(
                "status", "SHIPPED",
                "shipment_id", event.shipmentId() != null ? event.shipmentId() : ""));
    }

    @EventListener
    public void onOrderCompleted(OrderCompletedIntegrationEvent event) {
        dispatchIfWebhookRegistered(event.orderId(), "order.completed", Map.of(
                "status", "COMPLETED"));
    }

    @EventListener
    public void onOrderCancelled(OrderCancelledIntegrationEvent event) {
        dispatchIfWebhookRegistered(event.orderId(), "order.cancelled", Map.of(
                "status", "CANCELLED",
                "reason_code", event.reasonCode() != null ? event.reasonCode() : "",
                "reason", event.reason() != null ? event.reason() : ""));
    }

    private void dispatchIfWebhookRegistered(String orderId, String eventType, Map<String, Object> data) {
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
                IdGenerator.ulid(),
                eventType,
                orderId,
                clock.instant(),
                data);

        webhookDispatcher.dispatch(webhookUrl, webhookEvent);
    }

    private String extractWebhookUrl(UcpCheckoutSession session) {
        if (session.context() == null) {
            return null;
        }
        Object url = session.context().get("webhook_url");
        return url instanceof String s && !s.isBlank() ? s : null;
    }
}
