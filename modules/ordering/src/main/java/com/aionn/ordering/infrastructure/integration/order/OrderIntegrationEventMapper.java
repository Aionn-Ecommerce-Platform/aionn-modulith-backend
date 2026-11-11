package com.aionn.ordering.infrastructure.integration.order;

import java.util.UUID;

import com.aionn.ordering.domain.event.OrderEvents;
import com.aionn.contracts.event.ordering.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** Maps Ordering domain events to transport-safe integration events with a fresh eventId. */
@Component
public class OrderIntegrationEventMapper {

    public OrderPlacedIntegrationEvent toIntegrationEvent(OrderEvents.OrderPlaced domainEvent) {
        return new OrderPlacedIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.userId(),
                domainEvent.merchantId(),
                domainEvent.proposalId(),
                domainEvent.items().stream()
                        .map(item -> new OrderPlacedIntegrationEvent.OrderLineItem(
                                item.skuId(),
                                item.qty(),
                                item.unitPrice(),
                                item.warehouseId(),
                                item.reservationId()))
                        .collect(Collectors.toList()),
                domainEvent.totalAmount(),
                domainEvent.currency(),
                domainEvent.addressId(),
                domainEvent.paymentMethodId(),
                domainEvent.occurredAt());
    }

    public OrderApprovedIntegrationEvent toIntegrationEvent(OrderEvents.OrderApproved domainEvent) {
        return new OrderApprovedIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.paymentId(),
                domainEvent.occurredAt());
    }

    public OrderShippedIntegrationEvent toIntegrationEvent(OrderEvents.OrderShipped domainEvent) {
        return new OrderShippedIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.shipmentId(),
                domainEvent.occurredAt());
    }

    public OrderCompletedIntegrationEvent toIntegrationEvent(OrderEvents.OrderCompleted domainEvent) {
        return new OrderCompletedIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderCancelled domainEvent) {
        return new OrderCancelledIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.reasonCode(),
                domainEvent.reason(),
                OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED,
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderAutoCancelled domainEvent) {
        return new OrderCancelledIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                domainEvent.reasonCode(),
                "Auto-cancelled by system",
                OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED,
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderRejectedByMerchant domainEvent) {
        return new OrderCancelledIntegrationEvent(
                UUID.randomUUID().toString(),
                domainEvent.orderId(),
                "MERCHANT_REJECTED",
                domainEvent.reason(),
                OrderCancelledIntegrationEvent.CancellationType.MERCHANT_REJECTED,
                domainEvent.occurredAt());
    }
}
