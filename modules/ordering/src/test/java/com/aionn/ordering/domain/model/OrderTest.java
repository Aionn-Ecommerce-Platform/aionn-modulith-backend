package com.aionn.ordering.domain.model;

import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private static ShippingAddress address() {
        return new ShippingAddress("addr-1", "User", "+84912345678",
                "12 main st", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item(int qty) {
        return new OrderItem("sku-1", qty, Money.of(BigDecimal.valueOf(100), "VND"),
                "wh-1", "res-1");
    }

    private static Order pendingOrder() {
        Money subtotal = Money.of(BigDecimal.valueOf(200), "VND");
        Money shipping = Money.of(BigDecimal.valueOf(20), "VND");
        return Order.place("order-1", "user-1", "merchant-1", "prop-1",
                "COD", "VND", List.of(item(2)), address(), shipping, subtotal);
    }

    @Test
    void placeStartsInPendingAndComputesTotal() {
        Order order = pendingOrder();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("order-1", order.getOrderId());
        assertEquals(0, order.getTotalAmount().amount().compareTo(BigDecimal.valueOf(220)));
        assertNotNull(order.getCreatedAt());
        assertEquals(1, order.items().size());
    }

    @Test
    void placeRequiresAtLeastOneItem() {
        Money zero = Money.zero("VND");

        assertThrows(OrderingException.class,
                () -> Order.place("o", "u", "m", "p", "COD", "VND",
                        List.of(), address(), zero, zero));
    }

    @Test
    void approveTransitionsPendingToApproved() {
        Order order = pendingOrder();

        order.approve("payment-1");

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals("payment-1", order.getPaymentId());
    }

    @Test
    void confirmPreparationRequiresApprovedStatus() {
        Order order = pendingOrder();

        OrderingException ex = assertThrows(OrderingException.class, order::confirmPreparation);
        assertTrue(ex.getMessage().toLowerCase().contains("approved"));
    }

    @Test
    void approvedOrderCanBeConfirmedForPreparation() {
        Order order = pendingOrder();
        order.approve("payment-1");

        order.confirmPreparation();

        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void cancelPendingOrderRecordsReason() {
        Order order = pendingOrder();

        order.cancel("USER_CANCELLED", "changed mind");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("USER_CANCELLED", order.getReasonCode());
        assertNotNull(order.getCancelledAt());
    }

    @Test
    void completedOrderCannotTransition() {
        Order order = pendingOrder();
        order.approve("p");
        order.confirmPreparation();
        order.markShipped("ship-1");
        order.complete();

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertThrows(OrderingException.class, () -> order.cancel("X", "n"));
    }

    @Test
    void rejectByMerchantRequiresMatchingMerchantId() {
        Order order = pendingOrder();

        assertThrows(OrderingException.class,
                () -> order.rejectByMerchant("other-merchant", "no stock"));

        order.rejectByMerchant("merchant-1", "no stock");
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertEquals("MERCHANT_REJECTED", order.getReasonCode());
    }

    @Test
    void changeShippingInfoRecomputesTotal() {
        Order order = pendingOrder();
        Money newShipping = Money.of(BigDecimal.valueOf(50), "VND");
        ShippingAddress newAddr = address();

        order.changeShippingInfo(newAddr, newShipping);

        // 200 (subtotal) + 50 (new shipping) = 250
        assertEquals(0, order.getTotalAmount().amount().compareTo(BigDecimal.valueOf(250)));
        assertNull(order.getCancelledAt());
    }
}
