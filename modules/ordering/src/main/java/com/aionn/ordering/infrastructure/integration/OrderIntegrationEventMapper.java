package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.domain.event.OrderEvents;
import com.aionn.sharedkernel.integration.event.ordering.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Maps Ordering domain events to integration events.
 * 
 * <p>
 * This mapper extracts only the necessary data from domain events
 * and converts them into integration events that can be consumed by other
 * modules.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Convert domain-specific types to simple DTOs</li>
 * <li>Generate eventId for each integration event</li>
 * <li>Preserve occurredAt timestamp</li>
 * <li>Avoid leaking internal domain details</li>
 * </ul>
 */
@Component
public class OrderIntegrationEventMapper {

    public OrderPlacedIntegrationEvent toIntegrationEvent(OrderEvents.OrderPlaced domainEvent) {
        return new OrderPlacedIntegrationEvent(
                null, // eventId will be auto-generated
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
                null,
                domainEvent.orderId(),
                domainEvent.paymentId(),
                domainEvent.occurredAt());
    }

    public OrderShippedIntegrationEvent toIntegrationEvent(OrderEvents.OrderShipped domainEvent) {
        return new OrderShippedIntegrationEvent(
                null,
                domainEvent.orderId(),
                domainEvent.shipmentId(),
                domainEvent.occurredAt());
    }

    public OrderCompletedIntegrationEvent toIntegrationEvent(OrderEvents.OrderCompleted domainEvent) {
        return new OrderCompletedIntegrationEvent(
                null,
                domainEvent.orderId(),
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderCancelled domainEvent) {
        return new OrderCancelledIntegrationEvent(
                null,
                domainEvent.orderId(),
                domainEvent.reasonCode(),
                domainEvent.reason(),
                OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED,
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderAutoCancelled domainEvent) {
        return new OrderCancelledIntegrationEvent(
                null,
                domainEvent.orderId(),
                domainEvent.reasonCode(),
                "Auto-cancelled by system",
                OrderCancelledIntegrationEvent.CancellationType.AUTO_CANCELLED,
                domainEvent.occurredAt());
    }

    public OrderCancelledIntegrationEvent toIntegrationEvent(OrderEvents.OrderRejectedByMerchant domainEvent) {
        return new OrderCancelledIntegrationEvent(
                null,
                domainEvent.orderId(),
                "MERCHANT_REJECTED",
                domainEvent.reason(),
                OrderCancelledIntegrationEvent.CancellationType.MERCHANT_REJECTED,
                domainEvent.occurredAt());
    }
}
