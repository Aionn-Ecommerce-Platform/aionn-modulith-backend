package com.aionn.sharedkernel.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
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
        IntegrationEvent.PaymentScoped payment() { return scoped("paymentId", "payment-1"); }
        IntegrationEvent.ShipmentScoped shipment() { return scoped("shipmentId", "shipment-1"); }
        IntegrationEvent.MerchantScoped merchant() { return scoped("merchantId", "merchant-1"); }
        IntegrationEvent.ProductScoped product() { return scoped("productId", "product-1"); }
        IntegrationEvent.ConversationScoped conversation() { return scoped("conversationId", "conversation-1"); }
        IntegrationEvent.UserScoped user() { return scoped("userId", "user-1"); }
        IntegrationEvent.ReservationScoped reservation() { return scoped("reservationId", "reservation-1"); }
        IntegrationEvent.WarehouseScoped warehouse() { return scoped("warehouseId", "warehouse-1"); }
    }

    @SuppressWarnings("unchecked")
    private static <T extends IntegrationEvent> T scoped(String idMethod, String id) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(OutboxContractTest.class.getClassLoader(),
                new Class<?>[] { switch (idMethod) {
                    case "paymentId" -> IntegrationEvent.PaymentScoped.class;
                    case "shipmentId" -> IntegrationEvent.ShipmentScoped.class;
                    case "merchantId" -> IntegrationEvent.MerchantScoped.class;
                    case "productId" -> IntegrationEvent.ProductScoped.class;
                    case "conversationId" -> IntegrationEvent.ConversationScoped.class;
                    case "userId" -> IntegrationEvent.UserScoped.class;
                    case "reservationId" -> IntegrationEvent.ReservationScoped.class;
                    default -> IntegrationEvent.WarehouseScoped.class;
                } }, (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if (method.getName().equals(idMethod)) return id;
                    if (method.getName().equals("eventId")) return "evt";
                    if (method.getName().equals("occurredAt")) return Instant.EPOCH;
                    return null;
                });
    }

    @Test
    void createsDedicatedScheduler() {
        var scheduler = new OutboxSchedulingConfiguration().outboxTaskScheduler();
        assertEquals(1, scheduler.getPoolSize());
    }
}
