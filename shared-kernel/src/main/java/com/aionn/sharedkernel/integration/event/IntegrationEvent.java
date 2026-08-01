package com.aionn.sharedkernel.integration.event;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface IntegrationEvent {

    String eventId();

    Instant occurredAt();

    default String eventType() {
        return this.getClass().getName();
    }

    /**
     * Aggregate metadata used to order outbox delivery. Events that belong to a shared aggregate
     * stream should override these defaults explicitly.
     */
    default String aggregateType() {
        return eventType();
    }

    default String aggregateId() {
        return eventId();
    }

    interface OrderScoped extends IntegrationEvent {
        String orderId();
        @Override default String aggregateType() { return "Order"; }
        @Override default String aggregateId() { return orderId(); }
    }

    interface PaymentScoped extends IntegrationEvent {
        String paymentId();
        @Override default String aggregateType() { return "Payment"; }
        @Override default String aggregateId() { return paymentId(); }
    }

    interface ShipmentScoped extends IntegrationEvent {
        String shipmentId();
        @Override default String aggregateType() { return "Shipment"; }
        @Override default String aggregateId() { return shipmentId(); }
    }

    interface MerchantScoped extends IntegrationEvent {
        String merchantId();
        @Override default String aggregateType() { return "Merchant"; }
        @Override default String aggregateId() { return merchantId(); }
    }

    interface ProductScoped extends IntegrationEvent {
        String productId();
        @Override default String aggregateType() { return "Product"; }
        @Override default String aggregateId() { return productId(); }
    }

    interface ConversationScoped extends IntegrationEvent {
        String conversationId();
        @Override default String aggregateType() { return "Conversation"; }
        @Override default String aggregateId() { return conversationId(); }
    }

    interface UserScoped extends IntegrationEvent {
        String userId();
        @Override default String aggregateType() { return "User"; }
        @Override default String aggregateId() { return userId(); }
    }

    interface ReservationScoped extends IntegrationEvent {
        String reservationId();
        @Override default String aggregateType() { return "Reservation"; }
        @Override default String aggregateId() { return reservationId(); }
    }

    interface WarehouseScoped extends IntegrationEvent {
        String warehouseId();
        @Override default String aggregateType() { return "Warehouse"; }
        @Override default String aggregateId() { return warehouseId(); }
    }

    static String requireEventId(String eventId) {
        String normalized = Objects.requireNonNull(eventId, "eventId must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return normalized;
    }

    static Instant defaultOccurredAt(Instant occurredAt) {
        return occurredAt != null ? occurredAt : Instant.now(Clock.systemUTC());
    }

    static <T> List<T> freezeList(List<T> items, String fieldName) {
        Objects.requireNonNull(items, fieldName + " must not be null");
        return List.copyOf(items);
    }
}
