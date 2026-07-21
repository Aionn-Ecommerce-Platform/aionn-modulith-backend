package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort.CancellationKind;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAutoCancelWorkerTest {

    @Mock
    private OrderPersistencePort orderRepository;

    @Mock
    private StockReservationGateway stockReservationGateway;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OrderingIntegrationEventPublisherPort integrationEventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private OrderAutoCancelWorker worker;

    @Test
    void cancelsOrderAndReleasesReservations() {
        String orderId = "order-1";
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Order order = mock(Order.class);
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);
        Order savedOrder = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clock.instant()).thenReturn(now);
        when(order.items()).thenReturn(List.of(item1, item2));
        when(item1.reservationId()).thenReturn("res-1");
        when(item2.reservationId()).thenReturn("res-2");
        when(order.pullEvents()).thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(savedOrder.getOrderId()).thenReturn(orderId);

        worker.cancelOneExpired(orderId);

        verify(order).autoCancel("PAYMENT_TIMEOUT", now);
        verify(stockReservationGateway).release("res-1", "auto-cancel");
        verify(stockReservationGateway).release("res-2", "auto-cancel");
        verify(orderRepository).save(order);
        verify(eventPublisher).publish(anyList());
        verify(integrationEventPublisher).publishOrderCancelled(
                eq(orderId), eq("PAYMENT_TIMEOUT"), eq("Payment timeout"), eq(CancellationKind.AUTO_CANCELLED));
    }

    @Test
    void throwsExceptionWhenOrderNotFound() {
        String orderId = "non-existent";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> worker.cancelOneExpired(orderId))
                .isInstanceOf(OrderingException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void skipsItemsWithoutReservationId() {
        String orderId = "order-1";
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Order order = mock(Order.class);
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);
        Order savedOrder = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clock.instant()).thenReturn(now);
        when(order.items()).thenReturn(List.of(item1, item2));
        when(item1.reservationId()).thenReturn("res-1");
        when(item2.reservationId()).thenReturn(null); // No reservation
        when(order.pullEvents()).thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(savedOrder.getOrderId()).thenReturn(orderId);

        worker.cancelOneExpired(orderId);

        verify(stockReservationGateway).release("res-1", "auto-cancel");
        verify(stockReservationGateway, never()).release(eq((String) null), anyString());
    }

    @Test
    void continuesWhenReservationReleaseFailsForOneItem() {
        String orderId = "order-1";
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Order order = mock(Order.class);
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);
        Order savedOrder = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clock.instant()).thenReturn(now);
        when(order.items()).thenReturn(List.of(item1, item2));
        when(item1.reservationId()).thenReturn("res-1");
        when(item2.reservationId()).thenReturn("res-2");
        when(order.pullEvents()).thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(savedOrder.getOrderId()).thenReturn(orderId);
        doThrow(new RuntimeException("Inventory service unavailable"))
                .when(stockReservationGateway).release("res-1", "auto-cancel");

        worker.cancelOneExpired(orderId);

        verify(stockReservationGateway).release("res-1", "auto-cancel");
        verify(stockReservationGateway).release("res-2", "auto-cancel");
        verify(orderRepository).save(order);
        verify(integrationEventPublisher).publishOrderCancelled(
                anyString(), anyString(), anyString(), any(CancellationKind.class));
    }

    @Test
    void publishesDomainEventsAfterSave() {
        String orderId = "order-1";
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Order order = mock(Order.class);
        Order savedOrder = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clock.instant()).thenReturn(now);
        when(order.items()).thenReturn(List.of());
        when(order.pullEvents()).thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(savedOrder.getOrderId()).thenReturn(orderId);

        worker.cancelOneExpired(orderId);

        verify(eventPublisher).publish(anyList());
    }

    @Test
    void publishesIntegrationEventWithCorrectParameters() {
        String orderId = "order-1";
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        Order order = mock(Order.class);
        Order savedOrder = mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clock.instant()).thenReturn(now);
        when(order.items()).thenReturn(List.of());
        when(order.pullEvents()).thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(savedOrder.getOrderId()).thenReturn(orderId);

        worker.cancelOneExpired(orderId);

        verify(integrationEventPublisher).publishOrderCancelled(
                eq(orderId),
                eq("PAYMENT_TIMEOUT"),
                eq("Payment timeout"),
                eq(CancellationKind.AUTO_CANCELLED));
    }
}
