package com.aionn.sharedkernel.integration.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionn.sharedkernel.integration.event.catalog.MerchantActivatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantClosedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantSuspendedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.ProductEmergencyTakedownIntegrationEvent;
import com.aionn.sharedkernel.integration.event.chat.MessageSentIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.EmailChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PasswordChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PhoneChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.SafetyStockBreachedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReleasedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderShippedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentCapturedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentInitiatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentPaidIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentRefundedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveryFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDispatchedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentRegisteredIntegrationEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationEventContractTest {

    private static final Instant NOW = Instant.parse("2026-06-30T00:00:00Z");

    @Test
    void utilityMethodsValidateAndNormalizeInputs() {
        assertEquals("evt-1", IntegrationEvent.requireEventId("  evt-1  "));
        assertThrows(NullPointerException.class, () -> IntegrationEvent.requireEventId(null));
        assertThrows(IllegalArgumentException.class, () -> IntegrationEvent.requireEventId("   "));

        assertEquals(NOW, IntegrationEvent.defaultOccurredAt(NOW));
        assertNotNull(IntegrationEvent.defaultOccurredAt(null));

        List<String> frozen = IntegrationEvent.freezeList(new ArrayList<>(List.of("a", "b")), "items");
        assertEquals(List.of("a", "b"), frozen);
        assertThrows(UnsupportedOperationException.class, () -> frozen.add("c"));
        assertThrows(NullPointerException.class, () -> IntegrationEvent.freezeList(null, "items"));
    }

    @Test
    void integrationEventsInstantiateAndDefaultOccurredAt() {
        assertNotNull(new MerchantActivatedIntegrationEvent("evt-1", "m-1", "admin-1", null).occurredAt());
        assertNotNull(new MerchantClosedIntegrationEvent("evt-2", "m-1", "reason", null).occurredAt());
        assertNotNull(new MerchantSuspendedIntegrationEvent("evt-3", "m-1", "reason", null).occurredAt());
        assertNotNull(new ProductEmergencyTakedownIntegrationEvent("evt-4", "p-1", "admin-1", "reason", null).occurredAt());
        assertNotNull(new MessageSentIntegrationEvent("evt-5", "c-1", "msg-1", "sender-1", "recipient-1", "Merchant A", "hello", null).occurredAt());
        assertNotNull(new EmailChangedIntegrationEvent("evt-6", "u-1", "old@aionn.com", "new@aionn.com", null).occurredAt());
        assertNotNull(new PasswordChangedIntegrationEvent("evt-7", "u-1", "email", null).occurredAt());
        assertNotNull(new PhoneChangedIntegrationEvent("evt-8", "u-1", "old", "new", null).occurredAt());
        assertNotNull(new SafetyStockBreachedIntegrationEvent("evt-9", null, "m-1", "sku-1", "wh-1", 1, 2).occurredAt());
        assertNotNull(new OrderApprovedIntegrationEvent("evt-10", "o-1", "pay-1", null).occurredAt());
        assertNotNull(new OrderCancelledIntegrationEvent("evt-11", "o-1", "USER", "cancel", OrderCancelledIntegrationEvent.CancellationType.USER_CANCELLED, null).occurredAt());
        assertNotNull(new OrderCompletedIntegrationEvent("evt-12", "o-1", null).occurredAt());
        assertNotNull(new OrderShippedIntegrationEvent("evt-13", "o-1", "ship-1", null).occurredAt());
        assertNotNull(new PaymentFailedIntegrationEvent("evt-14", "pay-1", "o-1", "DECLINED", "reason", null).occurredAt());
        assertNotNull(new ShipmentCancelledIntegrationEvent("evt-15", "ship-1", "o-1", "reason", null).occurredAt());
        assertNotNull(new ShipmentDeliveredIntegrationEvent("evt-16", "ship-1", "o-1", "sig", NOW, null).occurredAt());
        assertNotNull(new ShipmentDeliveryFailedIntegrationEvent("evt-17", "ship-1", "o-1", "reason", 2, null).occurredAt());
        assertNotNull(new ShipmentDispatchedIntegrationEvent("evt-18", "ship-1", "o-1", "track", null).occurredAt());
        assertNotNull(new ShipmentRegisteredIntegrationEvent("evt-19", "ship-1", "o-1", "track", "carrier-1", null).occurredAt());
    }

    @Test
    void eventsWithValidationEnforceContract() {
        BigDecimal negativeAmount = BigDecimal.valueOf(-1);
        String blankCurrency = " ";

        assertThrows(IllegalArgumentException.class,
                () -> new StockCommittedIntegrationEvent("evt", "res", "sku", "wh", "order", 0, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new StockReleasedIntegrationEvent("evt", "res", "sku", "wh", "order", 0, "reason", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new StockReservationFailedIntegrationEvent("evt", "sku", "wh", "order", 0, "reason", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new StockReservedIntegrationEvent("evt", "res", "sku", "wh", "order", 0, NOW, NOW));

        assertThrows(IllegalArgumentException.class,
                () -> new PaymentCapturedIntegrationEvent("evt", "pay", "order", "txn", negativeAmount, "VND", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentInitiatedIntegrationEvent("evt", "pay", "order", BigDecimal.ONE, blankCurrency, "stripe", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentPaidIntegrationEvent("evt", "pay", "order", BigDecimal.ONE, blankCurrency, "stripe", "txn", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentRefundedIntegrationEvent("evt", "pay", "order", "refund", negativeAmount, "VND", "reason", NOW));
    }

    @Test
    void orderPlacedEventFreezesItemsAndUsesClassNameAsEventType() {
        var mutableItems = new ArrayList<>(List.of(
                new OrderPlacedIntegrationEvent.OrderLineItem("sku-1", 2, BigDecimal.TEN, "wh-1", "res-1")));

        OrderPlacedIntegrationEvent event = new OrderPlacedIntegrationEvent(
                "evt-20",
                "o-1",
                "u-1",
                "m-1",
                "proposal-1",
                mutableItems,
                BigDecimal.valueOf(99),
                "VND",
                "addr-1",
                "pm-1",
                null);

        mutableItems.add(new OrderPlacedIntegrationEvent.OrderLineItem("sku-2", 1, BigDecimal.ONE, "wh-2", "res-2"));

        assertEquals(1, event.items().size());
        OrderPlacedIntegrationEvent.OrderLineItem extraLineItem =
                new OrderPlacedIntegrationEvent.OrderLineItem("sku-3", 1, BigDecimal.ONE, "wh-3", "res-3");
        assertThrows(UnsupportedOperationException.class,
                () -> event.items().add(extraLineItem));
        assertEquals(OrderPlacedIntegrationEvent.class.getName(), event.eventType());
    }

    @Test
    void paymentEventsAcceptZeroAmount() {
        assertEquals(BigDecimal.ZERO, new PaymentCapturedIntegrationEvent(
                "evt-21", "pay-1", "o-1", "txn-1", BigDecimal.ZERO, "VND", NOW).amount());
        assertEquals(BigDecimal.ZERO, new PaymentInitiatedIntegrationEvent(
                "evt-22", "pay-1", "o-1", BigDecimal.ZERO, "VND", "stripe", NOW).amount());
        assertEquals(BigDecimal.ZERO, new PaymentPaidIntegrationEvent(
                "evt-23", "pay-1", "o-1", BigDecimal.ZERO, "VND", "stripe", "txn-1", NOW).amount());
        assertEquals(BigDecimal.ZERO, new PaymentRefundedIntegrationEvent(
                "evt-24", "pay-1", "o-1", "refund-1", BigDecimal.ZERO, "VND", "reason", NOW).amount());
    }
}
