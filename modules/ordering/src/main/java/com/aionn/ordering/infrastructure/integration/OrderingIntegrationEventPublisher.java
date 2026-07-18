package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderingIntegrationEventPublisher implements OrderingIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void publishOrderPlaced(Order order) {
        List<OrderPlacedIntegrationEvent.OrderLineItem> items = order.items().stream()
                .map(this::toLineItem).toList();
        integrationEventPublisher.publish(new OrderPlacedIntegrationEvent(
                IdGenerator.ulid(), order.getOrderId(), order.getUserId(), order.getMerchantId(),
                order.getProposalId(), items,
                order.getTotalAmount() == null ? null : order.getTotalAmount().amount(),
                order.getCurrency(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().addressId(),
                order.getPaymentMethodId(), Instant.now()));
    }

    @Override
    public void publishOrderApproved(String orderId, String paymentId) {
        integrationEventPublisher.publish(new OrderApprovedIntegrationEvent(
                IdGenerator.ulid(), orderId, paymentId, Instant.now()));
    }

    @Override
    public void publishOrderShipped(String orderId, String shipmentId) {
        integrationEventPublisher.publish(new OrderShippedIntegrationEvent(
                IdGenerator.ulid(), orderId, shipmentId, Instant.now()));
    }

    @Override
    public void publishOrderCompleted(String orderId) {
        integrationEventPublisher.publish(new OrderCompletedIntegrationEvent(
                IdGenerator.ulid(), orderId, Instant.now()));
    }

    @Override
    public void publishOrderCancelled(String orderId, String reasonCode, String reason, CancellationKind kind) {
        integrationEventPublisher.publish(new OrderCancelledIntegrationEvent(
                IdGenerator.ulid(), orderId, reasonCode, reason, mapKind(kind), Instant.now()));
    }

    private OrderPlacedIntegrationEvent.OrderLineItem toLineItem(OrderItem item) {
        return new OrderPlacedIntegrationEvent.OrderLineItem(
                item.skuId(), item.qty(),
                item.unitPrice() == null ? null : item.unitPrice().amount(),
                item.warehouseId(), item.reservationId());
    }

    private OrderCancelledIntegrationEvent.CancellationType mapKind(CancellationKind kind) {
        return switch (kind) {
            case USER_CANCELLED -> OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED;
            case AUTO_CANCELLED -> OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED;
            case MERCHANT_REJECTED -> OrderCancelledIntegrationEvent.CancellationType.MERCHANT_REJECTED;
        };
    }
}
