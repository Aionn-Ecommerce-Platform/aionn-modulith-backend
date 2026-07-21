package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.domain.event.OrderEvents;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderIntegrationEventPublisherTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @Mock
    private OrderIntegrationEventMapper mapper;

    @InjectMocks
    private OrderIntegrationEventPublisher publisher;

    @Test
    void onOrderPlacedPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderPlaced event = new OrderEvents.OrderPlaced(
                "ord-1", "usr-1", "m-1", "prop-1", List.of(), BigDecimal.TEN, "VND", "addr-1", "pm-1", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent(
                        "ev-1", "ord-1", "usr-1", "m-1", "prop-1", List.of(), BigDecimal.TEN, "VND", "addr-1", "pm-1", now));

        publisher.onOrderPlaced(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderApprovedPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderApproved event = new OrderEvents.OrderApproved("ord-1", "pay-1", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent(
                        "ev-1", "ord-1", "pay-1", now));

        publisher.onOrderApproved(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderCompletedPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderCompleted event = new OrderEvents.OrderCompleted("ord-1", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent("ev-1", "ord-1", now));

        publisher.onOrderCompleted(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderShippedPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderShipped event = new OrderEvents.OrderShipped("ord-1", "ship-1", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent("ev-1", "ord-1", "ship-1", now));

        publisher.onOrderShipped(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderCancelledPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderCancelled event = new OrderEvents.OrderCancelled("ord-1", "R1", "reason", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent(
                        "ev-1", "ord-1", "R1", "reason",
                        com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED, now));

        publisher.onOrderCancelled(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderAutoCancelledPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderAutoCancelled event = new OrderEvents.OrderAutoCancelled("ord-1", "EXP", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent(
                        "ev-1", "ord-1", "EXP", "Auto-cancelled",
                        com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED, now));

        publisher.onOrderAutoCancelled(event);

        verify(integrationEventPublisher).publish(any());
    }

    @Test
    void onOrderRejectedByMerchantPublishesIntegrationEvent() {
        Instant now = Instant.now();
        OrderEvents.OrderRejectedByMerchant event = new OrderEvents.OrderRejectedByMerchant("ord-1", "m-1", "reason", now, now);
        when(mapper.toIntegrationEvent(event)).thenReturn(
                new com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent(
                        "ev-1", "ord-1", "MERCHANT_REJECTED", "reason",
                        com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent.CancellationType.MERCHANT_REJECTED, now));

        publisher.onOrderRejectedByMerchant(event);

        verify(integrationEventPublisher).publish(any());
    }
}
