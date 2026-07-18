package com.aionn.ordering.domain.event;

import com.aionn.ordering.domain.valueobject.ShippingAddress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderEvents {

    private OrderEvents() {
    }

    public record OrderPlaced(
            String orderId,
            String userId,
            String merchantId,
            String proposalId,
            List<OrderLineSnapshot> items,
            BigDecimal totalAmount,
            String currency,
            String addressId,
            String paymentMethodId,
            Instant placedAt,
            Instant occurredAt) implements OrderingEvent {
    }

    public record OrderApproved(
            String orderId, String paymentId, Instant approvedAt, Instant occurredAt) implements OrderingEvent {
    }

    public record OrderPreparationConfirmed(
            String orderId, String merchantId, Instant confirmedAt, Instant occurredAt) implements OrderingEvent {
    }

    public record OrderShipped(
            String orderId, String shipmentId, Instant shippedAt, Instant occurredAt) implements OrderingEvent {
    }

    public record OrderCompleted(
            String orderId, Instant completedAt, Instant occurredAt) implements OrderingEvent {
    }

    public record OrderCancelled(
            String orderId, String reasonCode, String reason, Instant cancelledAt, Instant occurredAt)
            implements OrderingEvent {
    }

    public record OrderAutoCancelled(
            String orderId, String reasonCode, Instant cancelledAt, Instant occurredAt) implements OrderingEvent {
    }

    public record OrderRejectedByMerchant(
            String orderId, String merchantId, String reason, Instant rejectedAt, Instant occurredAt)
            implements OrderingEvent {
    }

    public record OrderShippingInfoChanged(
            String orderId,
            ShippingAddress newAddress,
            BigDecimal newShippingFee,
            String currency,
            Instant updatedAt,
            Instant occurredAt) implements OrderingEvent {
    }

    public record OrderSplit(
            String originalOrderId,
            List<String> splitOrderIds,
            String reason,
            Instant splitAt,
            Instant occurredAt) implements OrderingEvent {
    }

    public record OrderLineSnapshot(
            String skuId, int qty, BigDecimal unitPrice, String warehouseId, String reservationId) {
    }
}

