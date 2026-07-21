package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent;
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

@ExtendWith(MockitoExtension.class)
class OrderingIntegrationEventPublisherTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @InjectMocks
    private OrderingIntegrationEventPublisher publisher;

    private static ShippingAddress address() {
        return new ShippingAddress("a-1", "John", "+84912345678", "12 Main St", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", 2, Money.of(BigDecimal.valueOf(100), "VND"), "wh-1", "res-1");
    }

    @Test
    void publishOrderPlacedPublishesEvent() {
        Order order = Order.place("ord-1", "usr-1", "m-1", "prop-1", "pm-1", "VND",
                List.of(item()), address(), Money.zero("VND"), Money.of(BigDecimal.valueOf(200), "VND"), Instant.now());

        publisher.publishOrderPlaced(order);

        verify(integrationEventPublisher).publish(any(OrderPlacedIntegrationEvent.class));
    }

    @Test
    void publishOrderApprovedPublishesEvent() {
        publisher.publishOrderApproved("ord-1", "pay-100");

        verify(integrationEventPublisher).publish(any(OrderApprovedIntegrationEvent.class));
    }

    @Test
    void publishOrderShippedPublishesEvent() {
        publisher.publishOrderShipped("ord-1", "ship-100");

        verify(integrationEventPublisher).publish(any(OrderShippedIntegrationEvent.class));
    }

    @Test
    void publishOrderCompletedPublishesEvent() {
        publisher.publishOrderCompleted("ord-1");

        verify(integrationEventPublisher).publish(any(OrderCompletedIntegrationEvent.class));
    }

    @Test
    void publishOrderCancelledPublishesEvent() {
        publisher.publishOrderCancelled("ord-1", "PAYMENT_FAILED", "Timeout",
                OrderingIntegrationEventPublisherPort.CancellationKind.AUTO_CANCELLED);

        verify(integrationEventPublisher).publish(any(OrderCancelledIntegrationEvent.class));
    }
}
