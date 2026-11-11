package com.aionn.platform.infrastructure.outbox;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionn.contracts.event.IntegrationEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxContractTest {

    @Test
    void scopedContractsExposeExplicitAggregateMetadata() {
        ScopedEvent event = new ScopedEvent();
        assertEquals("Order", event.aggregateType());
        assertEquals("order-1", event.aggregateId());
        assertEquals("Payment", event.payment().aggregateType());
        assertEquals("Shipment", event.shipment().aggregateType());
        assertEquals("Merchant", event.merchant().aggregateType());
        assertEquals("Product", event.product().aggregateType());
        assertEquals("Conversation", event.conversation().aggregateType());
        assertEquals("User", event.user().aggregateType());
        assertEquals("Reservation", event.reservation().aggregateType());
        assertEquals("Warehouse", event.warehouse().aggregateType());
    }

    private record ScopedEvent() implements IntegrationEvent.OrderScoped {
        @Override public String eventId() { return "evt"; }
        @Override public String orderId() { return "order-1"; }
        @Override public Instant occurredAt() { return Instant.EPOCH; }
        IntegrationEvent.PaymentScoped payment() { return scoped(IntegrationEvent.PaymentScoped.class, "paymentId", "payment-1"); }
        IntegrationEvent.ShipmentScoped shipment() { return scoped(IntegrationEvent.ShipmentScoped.class, "shipmentId", "shipment-1"); }
        IntegrationEvent.MerchantScoped merchant() { return scoped(IntegrationEvent.MerchantScoped.class, "merchantId", "merchant-1"); }
        IntegrationEvent.ProductScoped product() { return scoped(IntegrationEvent.ProductScoped.class, "productId", "product-1"); }
        IntegrationEvent.ConversationScoped conversation() { return scoped(IntegrationEvent.ConversationScoped.class, "conversationId", "conversation-1"); }
        IntegrationEvent.UserScoped user() { return scoped(IntegrationEvent.UserScoped.class, "userId", "user-1"); }
        IntegrationEvent.ReservationScoped reservation() { return scoped(IntegrationEvent.ReservationScoped.class, "reservationId", "reservation-1"); }
        IntegrationEvent.WarehouseScoped warehouse() { return scoped(IntegrationEvent.WarehouseScoped.class, "warehouseId", "warehouse-1"); }
    }

    private static <T extends IntegrationEvent> T scoped(Class<T> eventType, String idMethod, String id) {
        Object proxy = Proxy.newProxyInstance(OutboxContractTest.class.getClassLoader(),
                new Class<?>[] { eventType }, (proxyInstance, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxyInstance, method, args);
                    }
                    if (method.getName().equals(idMethod)) return id;
                    if (method.getName().equals("eventId")) return "evt";
                    if (method.getName().equals("occurredAt")) return Instant.EPOCH;
                    return null;
                });
        return eventType.cast(proxy);
    }

    @Test
    void createsDedicatedScheduler() {
        var scheduler = new OutboxSchedulingConfiguration().outboxTaskScheduler();
        assertEquals(1, scheduler.getPoolSize());
    }
}
