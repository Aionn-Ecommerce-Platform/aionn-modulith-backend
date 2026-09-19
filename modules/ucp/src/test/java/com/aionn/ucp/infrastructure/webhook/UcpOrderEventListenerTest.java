package com.aionn.ucp.infrastructure.webhook;

import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpOrderEventListenerTest {

    @Mock
    private UcpCheckoutSessionPort sessionPort;

    @Mock
    private UcpWebhookDispatcherPort webhookDispatcher;

    private Clock clock;
    private UcpOrderEventListener listener;
    private final Instant now = Instant.parse("2026-09-19T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        listener = new UcpOrderEventListener(sessionPort, webhookDispatcher, clock);
    }

    private UcpCheckoutSession sampleSession(String orderId, String webhookUrl) {
        Map<String, Object> context = webhookUrl != null ? Map.of("webhook_url", webhookUrl) : Map.of();
        return new UcpCheckoutSession(
                "chk_1", "user_1", null, "completed", "VND", Map.of(),
                Map.of(), context, orderId, now, now, now.plusSeconds(3600));
    }

    @Test
    void onOrderPlacedDispatchesWebhookWhenRegistered() {
        String orderId = "ord_100";
        String webhookUrl = "https://agent.example/webhooks";
        when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(sampleSession(orderId, webhookUrl)));

        OrderPlacedIntegrationEvent event = new OrderPlacedIntegrationEvent(
                "evt_1", orderId, "user_1", "merch_1", "prop_1", List.of(),
                BigDecimal.valueOf(50000), "VND", "addr_1", "pm_1", now);

        listener.onOrderPlaced(event);

        ArgumentCaptor<UcpWebhookEvent> captor = ArgumentCaptor.forClass(UcpWebhookEvent.class);
        verify(webhookDispatcher).dispatch(eq(webhookUrl), captor.capture());

        UcpWebhookEvent dispatched = captor.getValue();
        assertThat(dispatched.eventType()).isEqualTo("order.placed");
        assertThat(dispatched.orderId()).isEqualTo(orderId);
        assertThat(dispatched.occurredAt()).isEqualTo(now);
    }

    @Test
    void onOrderShippedDispatchesWebhookWhenRegistered() {
        String orderId = "ord_200";
        String webhookUrl = "https://agent.example/webhooks";
        when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(sampleSession(orderId, webhookUrl)));

        OrderShippedIntegrationEvent event = new OrderShippedIntegrationEvent(
                "evt_2", orderId, "shipment_99", now);

        listener.onOrderShipped(event);

        ArgumentCaptor<UcpWebhookEvent> captor = ArgumentCaptor.forClass(UcpWebhookEvent.class);
        verify(webhookDispatcher).dispatch(eq(webhookUrl), captor.capture());

        UcpWebhookEvent dispatched = captor.getValue();
        assertThat(dispatched.eventType()).isEqualTo("order.shipped");
        assertThat(dispatched.data().get("shipment_id")).isEqualTo("shipment_99");
    }

    @Test
    void onOrderCompletedDispatchesWebhookWhenRegistered() {
        String orderId = "ord_300";
        String webhookUrl = "https://agent.example/webhooks";
        when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(sampleSession(orderId, webhookUrl)));

        OrderCompletedIntegrationEvent event = new OrderCompletedIntegrationEvent("evt_3", orderId, now);

        listener.onOrderCompleted(event);

        ArgumentCaptor<UcpWebhookEvent> captor = ArgumentCaptor.forClass(UcpWebhookEvent.class);
        verify(webhookDispatcher).dispatch(eq(webhookUrl), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("order.completed");
    }

    @Test
    void onOrderCancelledDispatchesWebhookWhenRegistered() {
        String orderId = "ord_400";
        String webhookUrl = "https://agent.example/webhooks";
        when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(sampleSession(orderId, webhookUrl)));

        OrderCancelledIntegrationEvent event = new OrderCancelledIntegrationEvent(
                "evt_4", orderId, "USER_CANCELLED", "Changed mind",
                OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED, now);

        listener.onOrderCancelled(event);

        ArgumentCaptor<UcpWebhookEvent> captor = ArgumentCaptor.forClass(UcpWebhookEvent.class);
        verify(webhookDispatcher).dispatch(eq(webhookUrl), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("order.cancelled");
        assertThat(captor.getValue().data().get("reason_code")).isEqualTo("USER_CANCELLED");
    }

    @Test
    void onOrderApprovedDispatchesWebhookWhenRegistered() {
        String orderId = "ord_500";
        String webhookUrl = "https://agent.example/webhooks";
        when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(sampleSession(orderId, webhookUrl)));

        OrderApprovedIntegrationEvent event = new OrderApprovedIntegrationEvent("evt_5", orderId, "pay_1", now);

        listener.onOrderApproved(event);

        verify(webhookDispatcher).dispatch(eq(webhookUrl), any(UcpWebhookEvent.class));
    }

    @Test
    void skipsDispatchWhenNoSessionFoundOrNoWebhookUrl() {
        when(sessionPort.findByOrderId("ord_missing")).thenReturn(Optional.empty());
        listener.onOrderCompleted(new OrderCompletedIntegrationEvent("evt_x", "ord_missing", now));
        verify(webhookDispatcher, never()).dispatch(any(), any());

        when(sessionPort.findByOrderId("ord_no_webhook"))
                .thenReturn(Optional.of(sampleSession("ord_no_webhook", null)));
        listener.onOrderCompleted(new OrderCompletedIntegrationEvent("evt_y", "ord_no_webhook", now));
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }
}
