package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReturnServiceTest {

    private static final String USER_ID = "user-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String ORDER_ID = "order-1";
    private static final String RETURN_ID = "return-1";

    @Mock private OrderPersistencePort orderRepository;
    @Mock private OrderReturnPersistencePort returnRepository;
    @Mock private OrderingResultMapper mapper;
    @Mock private EventPublisher eventPublisher;
    @Mock private MerchantQueryPort merchantQueryPort;
    @Mock private PaymentGateway paymentGateway;

    private OrderReturnService service;

    @BeforeEach
    void setUp() {
        service = new OrderReturnService(
                orderRepository, returnRepository, mapper, eventPublisher,
                merchantQueryPort, paymentGateway);
    }

    private static ShippingAddress address() {
        return new ShippingAddress("addr-1", "User", "+84912345678",
                "12 main", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", 1, Money.of(BigDecimal.valueOf(150), "VND"),
                "wh-1", null);
    }

    private static Order completedOrder() {
        Order order = Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1",
                "COD", "VND", List.of(item()), address(),
                Money.zero("VND"), Money.of(BigDecimal.valueOf(150), "VND"));
        order.approve("p");
        order.confirmPreparation();
        order.markShipped("ship-1");
        order.complete();
        return order;
    }

    private static OrderReturn requestedReturn() {
        return new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, null, null,
                ReturnStatus.REQUESTED, Instant.now(), null, null);
    }

    private static ReturnResult sampleResult(String status) {
        return new ReturnResult(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, null, null, null,
                status, Instant.now(), null, null);
    }

    @Test
    void requestReturnRejectsWhenOrderIsNotCompleted() {
        Order order = Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1",
                "COD", "VND", List.of(item()), address(),
                Money.zero("VND"), Money.of(BigDecimal.valueOf(150), "VND"));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class,
                () -> service.requestReturn(new RequestReturnCommand(
                        ORDER_ID, USER_ID, "broken", null)));
        verify(returnRepository, never()).save(any());
    }

    @Test
    void requestReturnRejectsForeignUser() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));

        assertThrows(OrderingException.class,
                () -> service.requestReturn(new RequestReturnCommand(
                        ORDER_ID, "intruder", "n", null)));
    }

    @Test
    void requestReturnPersistsAndReturnsResult() {
        Order order = completedOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(returnRepository.save(any(OrderReturn.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResult(any(OrderReturn.class))).thenReturn(sampleResult("REQUESTED"));

        ReturnResult result = service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null));

        assertEquals("REQUESTED", result.status());
        verify(returnRepository).save(any(OrderReturn.class));
    }

    @Test
    void rejectMovesReturnToRejected() {
        OrderReturn r = requestedReturn();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1"))
                .thenReturn(Optional.of(MERCHANT_ID));
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("REJECTED"));

        ReturnResult result = service.reject(new RejectReturnCommand(
                RETURN_ID, "owner-1", "no proof"));

        assertEquals("REJECTED", result.status());
        assertEquals(ReturnStatus.REJECTED, r.getStatus());
    }

    @Test
    void rejectFailsWhenOwnerHasNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1"))
                .thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> service.reject(new RejectReturnCommand(RETURN_ID, "owner-1", "n")));
    }

    @Test
    void getForRequesterReturnsResultForOwningUser() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(mapper.toResult(r)).thenReturn(sampleResult("REQUESTED"));

        ReturnResult result = service.getForRequester(RETURN_ID, USER_ID);

        assertEquals("REQUESTED", result.status());
    }

    @Test
    void getForRequesterRejectsUnrelatedUser() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder")).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> service.getForRequester(RETURN_ID, "intruder"));
    }

    @Test
    void listMineMapsResultsFromRepository() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findByUserId(USER_ID, 50)).thenReturn(List.of(r));
        when(mapper.toResult(r)).thenReturn(sampleResult("REQUESTED"));

        List<ReturnResult> results = service.listMine(USER_ID, 50);

        assertEquals(1, results.size());
        assertEquals("REQUESTED", results.get(0).status());
    }

    @Test
    void adminRejectThrowsWhenReturnNotFound() {
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> service.adminReject(RETURN_ID, "n"));
    }

    @Test
    void approveReturnChangesStatusToApproved() {
        OrderReturn r = requestedReturn();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("APPROVED"));
        // refundAmount required by domain model; order has no paymentId so refund is skipped
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand cmd =
                new com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand(
                        RETURN_ID, "owner-1", java.math.BigDecimal.valueOf(150), "VND", "wh-1");

        var result = service.approve(cmd);

        assertEquals("APPROVED", result.status());
        verify(returnRepository).save(r);
    }

    @Test
    void confirmItemReceivedChangesStateOnReturn() {
        OrderReturn r = new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, "wh-1", null,
                ReturnStatus.APPROVED, Instant.now(), null, null);
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("ITEM_RECEIVED"));

        var result = service.confirmItemReceived(
                new com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand(
                        RETURN_ID, "owner-1", "GOOD"));

        assertEquals("ITEM_RECEIVED", result.status());
    }

    @Test
    void listMerchantReturnsMerchantReturns() {
        OrderReturn r = requestedReturn();
        when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.of(MERCHANT_ID));
        when(returnRepository.findByMerchantId(MERCHANT_ID, 50)).thenReturn(java.util.List.of(r));
        when(mapper.toResult(r)).thenReturn(sampleResult("REQUESTED"));

        var results = service.listMerchant(USER_ID, 50);

        assertEquals(1, results.size());
    }

    @Test
    void listMerchantThrowsWhenNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class, () -> service.listMerchant(USER_ID, 10));
    }

    @Test
    void adminGetReturnsFetchedReturn() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(mapper.toResult(r)).thenReturn(sampleResult("REQUESTED"));

        var result = service.adminGet(RETURN_ID);

        assertEquals("REQUESTED", result.status());
    }

    @Test
    void adminGetThrowsWhenNotFound() {
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class, () -> service.adminGet(RETURN_ID));
    }

    @Test
    void adminRejectSucceeds() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("REJECTED"));

        var result = service.adminReject(RETURN_ID, "invalid claim");

        assertEquals("REJECTED", result.status());
    }

    @Test
    void adminApproveSucceeds() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("APPROVED"));
        // refundAmount required; order has no paymentId so refund call is skipped
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));

        var result = service.adminApprove(RETURN_ID, java.math.BigDecimal.valueOf(150), "VND", "wh-1");

        assertEquals("APPROVED", result.status());
    }

    @Test
    void adminConfirmItemReceivedSucceeds() {
        OrderReturn r = new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, "wh-1", null,
                ReturnStatus.APPROVED, Instant.now(), null, null);
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("ITEM_RECEIVED"));

        var result = service.adminConfirmItemReceived(RETURN_ID, "GOOD");

        assertEquals("ITEM_RECEIVED", result.status());
    }

    @Test
    void getForRequesterAllowsMerchantAccess() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(merchantQueryPort.findMerchantIdByOwnerId("merchant-owner"))
                .thenReturn(Optional.of(MERCHANT_ID));
        when(mapper.toResult(r)).thenReturn(sampleResult("REQUESTED"));

        var result = service.getForRequester(RETURN_ID, "merchant-owner");

        assertEquals("REQUESTED", result.status());
    }

    @Test
    void adminAnalyticsReturnsCorrectStats() {
        java.time.LocalDate to = java.time.LocalDate.now();
        java.time.LocalDate from = to.minusDays(5);
        var mockRows = List.of(
                new com.aionn.ordering.application.port.out.OrderReturnPersistencePort.ReturnAnalyticsRow(
                        "APPROVED", "damaged", java.math.BigDecimal.valueOf(100), "VND"),
                new com.aionn.ordering.application.port.out.OrderReturnPersistencePort.ReturnAnalyticsRow(
                        "REQUESTED", null, null, null)
        );
        when(returnRepository.findReturnAnalyticsRows(any(), any())).thenReturn(mockRows);
        when(returnRepository.countCompletedOrdersBetween(any(), any())).thenReturn(10L);

        var result = service.adminAnalytics(from, to);

        assertEquals(2, result.totalReturns());
        assertEquals(10L, result.totalCompletedOrders());
        assertEquals(0.2, result.returnRate());
        assertEquals(java.math.BigDecimal.valueOf(100), result.totalRefundAmount());
        assertEquals("VND", result.currency());
    }

    @Test
    void requestReturnThrowsExpiredWindow() {
        Order order = completedOrder();
        // Force completedAt to be 10 days ago
        java.lang.reflect.Field field;
        try {
            field = Order.class.getDeclaredField("completedAt");
            field.setAccessible(true);
            field.set(order, Instant.now().minus(java.time.Duration.ofDays(10)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "damaged", null)));
    }

    @Test
    void triggerRefundIfPaidExecutesPaymentGatewayRefund() {
        OrderReturn r = requestedReturn();
        when(returnRepository.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(returnRepository.save(r)).thenReturn(r);
        when(mapper.toResult(r)).thenReturn(sampleResult("APPROVED"));
        
        Order order = completedOrder();
        // set paymentId on order using reflection
        try {
            java.lang.reflect.Field field = Order.class.getDeclaredField("paymentId");
            field.setAccessible(true);
            field.set(order, "payment-123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        service.adminApprove(RETURN_ID, java.math.BigDecimal.valueOf(100), "VND", "wh-1");

        verify(paymentGateway).refund("payment-123", java.math.BigDecimal.valueOf(100), "VND", "return approved (admin)");
    }
}

