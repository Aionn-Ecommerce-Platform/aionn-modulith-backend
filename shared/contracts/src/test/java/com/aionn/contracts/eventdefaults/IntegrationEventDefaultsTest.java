package com.aionn.contracts.eventdefaults;

import com.aionn.contracts.event.IntegrationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventDefaultsTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-01T10:00:00Z");

    record PlainEvent(String eventId, Instant occurredAt) implements IntegrationEvent {
    }

    record OrderEvent(String eventId, Instant occurredAt, String orderId)
            implements IntegrationEvent.OrderScoped {
    }

    record PaymentEvent(String eventId, Instant occurredAt, String paymentId)
            implements IntegrationEvent.PaymentScoped {
    }

    record ShipmentEvent(String eventId, Instant occurredAt, String shipmentId)
            implements IntegrationEvent.ShipmentScoped {
    }

    record MerchantEvent(String eventId, Instant occurredAt, String merchantId)
            implements IntegrationEvent.MerchantScoped {
    }

    record ProductEvent(String eventId, Instant occurredAt, String productId)
            implements IntegrationEvent.ProductScoped {
    }

    record ConversationEvent(String eventId, Instant occurredAt, String conversationId)
            implements IntegrationEvent.ConversationScoped {
    }

    record UserEvent(String eventId, Instant occurredAt, String userId)
            implements IntegrationEvent.UserScoped {
    }

    record ReservationEvent(String eventId, Instant occurredAt, String reservationId)
            implements IntegrationEvent.ReservationScoped {
    }

    record WarehouseEvent(String eventId, Instant occurredAt, String warehouseId)
            implements IntegrationEvent.WarehouseScoped {
    }

    @Test
    void plainEventsUseTheirTypeAndIdAsTheAggregate() {
        PlainEvent event = new PlainEvent("evt-1", OCCURRED_AT);

        assertThat(event.eventType()).isEqualTo(PlainEvent.class.getName());
        assertThat(event.aggregateType()).isEqualTo(PlainEvent.class.getName());
        assertThat(event.aggregateId()).isEqualTo("evt-1");
    }

    @Test
    void scopedEventsUseTheScopedAggregate() {
        assertThat(new OrderEvent("evt-1", OCCURRED_AT, "ord-1").aggregateType()).isEqualTo("Order");
        assertThat(new OrderEvent("evt-1", OCCURRED_AT, "ord-1").aggregateId()).isEqualTo("ord-1");
        assertThat(new PaymentEvent("evt-1", OCCURRED_AT, "pay-1").aggregateType()).isEqualTo("Payment");
        assertThat(new PaymentEvent("evt-1", OCCURRED_AT, "pay-1").aggregateId()).isEqualTo("pay-1");
        assertThat(new ShipmentEvent("evt-1", OCCURRED_AT, "shp-1").aggregateType()).isEqualTo("Shipment");
        assertThat(new ShipmentEvent("evt-1", OCCURRED_AT, "shp-1").aggregateId()).isEqualTo("shp-1");
        assertThat(new MerchantEvent("evt-1", OCCURRED_AT, "mer-1").aggregateType()).isEqualTo("Merchant");
        assertThat(new MerchantEvent("evt-1", OCCURRED_AT, "mer-1").aggregateId()).isEqualTo("mer-1");
        assertThat(new ProductEvent("evt-1", OCCURRED_AT, "prd-1").aggregateType()).isEqualTo("Product");
        assertThat(new ProductEvent("evt-1", OCCURRED_AT, "prd-1").aggregateId()).isEqualTo("prd-1");
        assertThat(new ConversationEvent("evt-1", OCCURRED_AT, "cnv-1").aggregateType())
                .isEqualTo("Conversation");
        assertThat(new ConversationEvent("evt-1", OCCURRED_AT, "cnv-1").aggregateId()).isEqualTo("cnv-1");
        assertThat(new UserEvent("evt-1", OCCURRED_AT, "usr-1").aggregateType()).isEqualTo("User");
        assertThat(new UserEvent("evt-1", OCCURRED_AT, "usr-1").aggregateId()).isEqualTo("usr-1");
        assertThat(new ReservationEvent("evt-1", OCCURRED_AT, "rsv-1").aggregateType())
                .isEqualTo("Reservation");
        assertThat(new ReservationEvent("evt-1", OCCURRED_AT, "rsv-1").aggregateId()).isEqualTo("rsv-1");
        assertThat(new WarehouseEvent("evt-1", OCCURRED_AT, "wh-1").aggregateType()).isEqualTo("Warehouse");
        assertThat(new WarehouseEvent("evt-1", OCCURRED_AT, "wh-1").aggregateId()).isEqualTo("wh-1");
    }

    @Test
    void requireEventIdTrimsAndRejectsBlankValues() {
        assertThat(IntegrationEvent.requireEventId("  evt-1  ")).isEqualTo("evt-1");
        assertThatThrownBy(() -> IntegrationEvent.requireEventId(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> IntegrationEvent.requireEventId("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultOccurredAtFallsBackToNow() {
        assertThat(IntegrationEvent.defaultOccurredAt(null)).isNotNull();
        assertThat(IntegrationEvent.defaultOccurredAt(OCCURRED_AT)).isEqualTo(OCCURRED_AT);
    }

    @Test
    void freezeListCopiesAndRejectsNull() {
        assertThat(IntegrationEvent.freezeList(List.of("a", "b"), "items")).containsExactly("a", "b");
        assertThatThrownBy(() -> IntegrationEvent.freezeList(null, "items"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("items");
    }
}
