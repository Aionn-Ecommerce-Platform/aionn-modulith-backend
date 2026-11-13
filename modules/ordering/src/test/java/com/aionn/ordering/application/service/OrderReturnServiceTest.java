package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.domain.valueobject.ReturnRefundStatus;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.mockito.ArgumentMatchers;

import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnItemPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.policy.OrderDefaultsPolicy;
import com.aionn.ordering.application.policy.OrderingAnalyticsPolicy;
import com.aionn.ordering.application.policy.RefundRetryPolicy;
import com.aionn.ordering.application.policy.ReturnWindowPolicy;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReturnServiceTest {

    private static final String USER_ID = "user-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String ORDER_ID = "order-1";
    private static final String RETURN_ID = "return-1";

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private OrderReturnPersistencePort orderReturnPersistencePort;
    @Mock private OrderReturnItemPersistencePort orderReturnItemPersistencePort;
    @Mock private EventPublisher eventPublisher;
    @Mock private MerchantQueryPort merchantQueryPort;
    @Mock private ReturnWindowPolicy returnWindowPolicy;
    @Mock private RefundRetryPolicy refundRetryPolicy;
    @Mock private OrderDefaultsPolicy orderDefaultsPolicy;
    @Mock private OrderingAnalyticsPolicy analyticsPolicy;
    @Mock private PaymentGateway paymentGateway;
    @Mock private Clock clock;
    @Mock private TransactionTemplate transactionTemplate;

    private OrderReturnService service;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-07-18T12:00:00Z"));
        lenient().when(returnWindowPolicy.isWithinWindow(any(), any())).thenAnswer(invocation -> {
            Instant completedAt = invocation.getArgument(0);
            Instant now = invocation.getArgument(1);
            return !now.isAfter(completedAt.plus(Duration.ofDays(7)));
        });
        lenient().when(refundRetryPolicy.maxAttempts()).thenReturn(5);
        lenient().when(refundRetryPolicy.batchSize()).thenReturn(100);
        lenient().when(orderDefaultsPolicy.defaultCurrency()).thenReturn("VND");
        lenient().when(analyticsPolicy.zone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        lenient().when(analyticsPolicy.platformDefaultRangeDays()).thenReturn(30);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        service = new OrderReturnService(
                orderPersistencePort, orderReturnPersistencePort, orderReturnItemPersistencePort, eventPublisher,
                merchantQueryPort, returnWindowPolicy, refundRetryPolicy,
                orderDefaultsPolicy, analyticsPolicy, paymentGateway, clock, transactionTemplate);
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
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        Order order = Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1",
                "COD", "VND", List.of(item()), address(),
                Money.zero("VND"), Money.of(BigDecimal.valueOf(150), "VND"), now);
        order.approve("p", now);
        order.confirmPreparation(now);
        order.markShipped("ship-1", now);
        order.complete(now);
        return order;
    }

    private static OrderReturn requestedReturn() {
        return new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, null, null,
                ReturnStatus.REQUESTED, Instant.parse("2026-07-18T12:00:00Z"), null, null);
    }

    @Test
    void requestReturnRejectsWhenOrderIsNotCompleted() {
        Order order = Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1",
                "COD", "VND", List.of(item()), address(),
                Money.zero("VND"), Money.of(BigDecimal.valueOf(150), "VND"), Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class,
                () -> service.requestReturn(new RequestReturnCommand(
                        ORDER_ID, USER_ID, "broken", null)));
        verify(orderReturnPersistencePort, never()).save(any());
    }

    @Test
    void requestReturnRejectsForeignUser() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));

        assertThrows(OrderingException.class,
                () -> service.requestReturn(new RequestReturnCommand(
                        ORDER_ID, "intruder", "n", null)));
    }

    @Test
    void requestReturnPersistsAndReturnsResult() {
        Order order = completedOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderReturnPersistencePort.save(any(OrderReturn.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderReturn result = service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null));

        assertEquals(ReturnStatus.REQUESTED, result.getStatus());
        verify(orderReturnPersistencePort).save(any(OrderReturn.class));
    }

    @Test
    void rejectMovesReturnToRejected() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);

        OrderReturn result = service.reject(new RejectReturnCommand(
                RETURN_ID, MERCHANT_ID, "no proof"));

        assertEquals(ReturnStatus.REJECTED, result.getStatus());
        assertEquals(ReturnStatus.REJECTED, r.getStatus());
    }

    @Test
    void rejectFailsWhenMerchantDoesNotOwnReturn() {
        // Return belongs to MERCHANT_ID; a different merchantId must be rejected.
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        assertThrows(OrderingException.class,
                () -> service.reject(new RejectReturnCommand(RETURN_ID, "other-merchant", "n")));
    }

    @Test
    void getForRequesterReturnsResultForOwningUser() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        OrderReturn result = service.getForRequester(RETURN_ID, USER_ID);

        assertEquals(ReturnStatus.REQUESTED, result.getStatus());
    }

    @Test
    void getForRequesterRejectsUnrelatedUser() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder")).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> service.getForRequester(RETURN_ID, "intruder"));
    }

    @Test
    void listMineMapsResultsFromRepository() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findByUserId(USER_ID, 50)).thenReturn(List.of(r));

        List<OrderReturn> results = service.listMine(USER_ID, 50);

        assertEquals(1, results.size());
        assertEquals(ReturnStatus.REQUESTED, results.get(0).getStatus());
    }

    @Test
    void adminRejectThrowsWhenReturnNotFound() {
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> service.adminReject(RETURN_ID, "n"));
    }

    @Test
    void approveReturnChangesStatusToApproved() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);
        // refundAmount required by domain model; order has no paymentId so refund is skipped
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        ApproveReturnCommand cmd =
                new ApproveReturnCommand(
                        RETURN_ID, MERCHANT_ID, BigDecimal.valueOf(150), "VND", "wh-1");

        var result = service.approve(cmd);

        assertEquals(ReturnStatus.APPROVED, result.getStatus());
        assertEquals(ReturnRefundStatus.REFUNDED,
                result.getRefundStatus());
        verify(orderReturnPersistencePort, times(2)).save(r);
    }

    @Test
    void confirmItemReceivedChangesStateOnReturn() {
        OrderReturn r = new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, "wh-1", null,
                ReturnStatus.APPROVED, Instant.now(), null, null);
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);

        var result = service.confirmItemReceived(
                new ConfirmItemReceivedCommand(
                        RETURN_ID, MERCHANT_ID, "GOOD"));

        assertEquals(ReturnStatus.ITEM_RECEIVED, result.getStatus());
    }

    @Test
    void listMerchantReturnsMerchantReturns() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findByMerchantId(MERCHANT_ID, 50)).thenReturn(List.of(r));

        var results = service.listMerchant(MERCHANT_ID, 50);

        assertEquals(1, results.size());
    }

    @Test
    void listMerchantReturnsEmptyForUnknownMerchant() {
        when(orderReturnPersistencePort.findByMerchantId(MERCHANT_ID, 10)).thenReturn(List.of());

        assertEquals(0, service.listMerchant(MERCHANT_ID, 10).size());
    }

    @Test
    void adminGetReturnsFetchedReturn() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        var result = service.adminGet(RETURN_ID);

        assertEquals(ReturnStatus.REQUESTED, result.getStatus());
    }

    @Test
    void adminGetThrowsWhenNotFound() {
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class, () -> service.adminGet(RETURN_ID));
    }

    @Test
    void adminRejectSucceeds() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);

        var result = service.adminReject(RETURN_ID, "invalid claim");

        assertEquals(ReturnStatus.REJECTED, result.getStatus());
    }

    @Test
    void adminApproveSucceeds() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);
        // refundAmount required; order has no paymentId so refund call is skipped
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));

        var result = service.adminApprove(RETURN_ID, BigDecimal.valueOf(150), "VND", "wh-1");

        assertEquals(ReturnStatus.APPROVED, result.getStatus());
    }

    @Test
    void adminConfirmItemReceivedSucceeds() {
        OrderReturn r = new OrderReturn(RETURN_ID, ORDER_ID, USER_ID, MERCHANT_ID,
                "broken", null, null, null, "wh-1", null,
                ReturnStatus.APPROVED, Instant.now(), null, null);
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);

        var result = service.adminConfirmItemReceived(RETURN_ID, "GOOD");

        assertEquals(ReturnStatus.ITEM_RECEIVED, result.getStatus());
    }

    @Test
    void getForRequesterAllowsMerchantAccess() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(merchantQueryPort.findMerchantIdByOwnerId("merchant-owner"))
                .thenReturn(Optional.of(MERCHANT_ID));

        var result = service.getForRequester(RETURN_ID, "merchant-owner");

        assertEquals(ReturnStatus.REQUESTED, result.getStatus());
    }

    @Test
    void adminAnalyticsReturnsCorrectStats() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(5);
        var mockRows = List.of(
                new OrderReturnPersistencePort.ReturnAnalyticsRow(
                        "APPROVED", "damaged", BigDecimal.valueOf(100), "VND"),
                new OrderReturnPersistencePort.ReturnAnalyticsRow(
                        "REQUESTED", null, null, null)
        );
        when(orderReturnPersistencePort.findReturnAnalyticsRows(any(), any())).thenReturn(mockRows);
        when(orderReturnPersistencePort.countCompletedOrdersBetween(any(), any())).thenReturn(10L);

        var result = service.adminAnalytics(from, to);

        assertEquals(2, result.totalReturns());
        assertEquals(10L, result.totalCompletedOrders());
        assertEquals(0.2, result.returnRate());
        assertEquals(BigDecimal.valueOf(100), result.totalRefundAmount());
        assertEquals("VND", result.currency());
    }

    @Test
    void requestReturnThrowsExpiredWindow() {
        Order order = completedOrder();
        // Force completedAt to be 10 days prior to clock.instant() (2026-07-18T12:00:00Z)
        Field field;
        try {
            field = Order.class.getDeclaredField("completedAt");
            field.setAccessible(true);
            field.set(order, Instant.parse("2026-07-08T12:00:00Z"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "damaged", null)));
    }

    @Test
    void triggerRefundIfPaidExecutesPaymentGatewayRefund() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderReturnPersistencePort.save(r)).thenReturn(r);
        
        Order order = completedOrder();
        // set paymentId on order using reflection
        try {
            Field field = Order.class.getDeclaredField("paymentId");
            field.setAccessible(true);
            field.set(order, "payment-123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "VND", "wh-1");

        verify(paymentGateway).refund("payment-123", BigDecimal.valueOf(100), "VND",
                "return approved (admin)", "return:return-1:refund");
    }

    @Test
    void repeatedApprovalDoesNotRepeatCompletedRefund() {
        OrderReturn orderReturn = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(orderReturn));
        when(orderReturnPersistencePort.save(orderReturn)).thenReturn(orderReturn);
        Order order = completedOrder();
        try {
            Field field = Order.class.getDeclaredField("paymentId");
            field.setAccessible(true);
            field.set(order, "payment-123");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "VND", "wh-1");
        service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "VND", "wh-1");

        verify(paymentGateway).refund(
                "payment-123", BigDecimal.valueOf(100), "VND",
                "return approved (admin)", "return:return-1:refund");
        verify(orderReturnPersistencePort, times(2)).save(orderReturn);
    }

    @Test
    void failedRefundIsPersistedAndRetriedWithTheSameBusinessKey() {
        OrderReturn orderReturn = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(orderReturn));
        when(orderReturnPersistencePort.save(orderReturn)).thenReturn(orderReturn);
        Order order = completedOrder();
        try {
            Field field = Order.class.getDeclaredField("paymentId");
            field.setAccessible(true);
            field.set(order, "payment-123");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("provider timeout")).doNothing()
                .when(paymentGateway).refund(any(), any(), any(), any(), any());

        var failed = service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "VND", "wh-1");
        assertEquals(ReturnRefundStatus.FAILED,
                failed.getRefundStatus());
        assertEquals(1, failed.getRefundAttempts());

        when(clock.instant()).thenReturn(Instant.parse("2026-07-18T12:00:02Z"));
        var retried = service.retryRefund(RETURN_ID);

        assertEquals(ReturnRefundStatus.REFUNDED,
                retried.getRefundStatus());
        verify(paymentGateway, times(2)).refund(
                eq("payment-123"), eq(BigDecimal.valueOf(100)), eq("VND"),
                ArgumentMatchers.anyString(), eq("return:return-1:refund"));
    }

    @Test
    void requestReturnRejectsABlankSkuInTheSelection() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null,
                        List.of(new RequestReturnCommand.Item(" ", 1)))));

        verify(orderReturnItemPersistencePort, never()).saveAll(any(), any());
    }

    @Test
    void requestReturnRejectsDuplicateSelectionLines() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null,
                        List.of(new RequestReturnCommand.Item("sku-1", 1),
                                new RequestReturnCommand.Item("sku-1", 1)))));

        verify(orderReturnItemPersistencePort, never()).saveAll(any(), any());
    }

    @Test
    void requestReturnRejectsAnItemOutsideTheOrder() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null,
                        List.of(new RequestReturnCommand.Item("sku-9", 1)))));

        verify(orderReturnItemPersistencePort, never()).saveAll(any(), any());
    }

    @Test
    void requestReturnRejectsQuantityAboveTheOrderedAmount() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OrderingException.class, () -> service.requestReturn(
                new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null,
                        List.of(new RequestReturnCommand.Item("sku-1", 2)))));

        verify(orderReturnItemPersistencePort, never()).saveAll(any(), any());
    }

    @Test
    void requestReturnPersistsTheSelectedItemsWithTheirUnitPrice() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));
        java.util.concurrent.atomic.AtomicReference<List<OrderReturnItemPersistencePort.Item>> saved =
                new java.util.concurrent.atomic.AtomicReference<>();
        org.mockito.Mockito.doAnswer(inv -> {
            saved.set(inv.getArgument(1));
            return null;
        }).when(orderReturnItemPersistencePort).saveAll(any(), any());

        service.requestReturn(new RequestReturnCommand(ORDER_ID, USER_ID, "broken", null,
                List.of(new RequestReturnCommand.Item("sku-1", 1))));

        assertEquals(1, saved.get().size());
        assertEquals("sku-1", saved.get().get(0).skuId());
        assertEquals(1, saved.get().get(0).qty());
        assertEquals(0, saved.get().get(0).unitPrice().compareTo(BigDecimal.valueOf(150)));
        assertEquals(0, saved.get().get(0).refundAmount().compareTo(BigDecimal.valueOf(150)));
    }

    @Test
    void listSelectedItemsReturnsRowsAfterAccessCheck() {
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(requestedReturn()));
        List<OrderReturnItemPersistencePort.Item> rows = List.of(
                new OrderReturnItemPersistencePort.Item("sku-1", 1,
                        BigDecimal.valueOf(150), BigDecimal.valueOf(150)));
        when(orderReturnItemPersistencePort.findByReturnId(RETURN_ID)).thenReturn(rows);

        assertEquals(rows, service.listSelectedItems(RETURN_ID, USER_ID));
    }

    @Test
    void adminListByStatusDelegatesToTheRepository() {
        when(orderReturnPersistencePort.findByStatus(ReturnStatus.REQUESTED, 20))
                .thenReturn(List.of(requestedReturn()));

        assertEquals(1, service.adminListByStatus(ReturnStatus.REQUESTED, 20).size());
    }

    @Test
    void adminAnalyticsRejectsAnInvertedRange() {
        assertThrows(OrderingException.class, () -> service.adminAnalytics(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1)));
    }

    @Test
    void retryingARefundWithoutAStoredAmountIsANoOp() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        OrderReturn result = service.retryRefund(RETURN_ID);

        assertEquals(ReturnStatus.REQUESTED, result.getStatus());
        verify(paymentGateway, never()).refund(any(), any(), any(), any(), any());
    }

    @Test
    void approvingAnAlreadyApprovedReturnWithTheSameDetailsIsIdempotent() {
        OrderReturn r = alreadyApproved("VND", "wh-1");
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderReturn result = service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "vnd", "wh-1");

        assertEquals(ReturnStatus.APPROVED, result.getStatus());
        assertEquals(ReturnRefundStatus.REFUNDED, result.getRefundStatus());
    }

    @Test
    void approvingAnAlreadyApprovedReturnWithADifferentCurrencyIsRejected() {
        OrderReturn r = alreadyApproved("VND", "wh-1");
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        assertThrows(OrderingException.class,
                () -> service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "USD", "wh-1"));
    }

    @Test
    void approvingAnAlreadyApprovedReturnWithADifferentWarehouseIsRejected() {
        OrderReturn r = alreadyApproved("VND", "wh-1");
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        assertThrows(OrderingException.class,
                () -> service.adminApprove(RETURN_ID, BigDecimal.valueOf(100), "VND", "wh-2"));
    }

    @Test
    void approvingAReturnWithoutACurrencyUsesTheDefaultCurrency() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderReturn result = service.adminApprove(RETURN_ID, BigDecimal.valueOf(50), null, "wh-1");

        assertEquals("VND", result.getRefundAmount().currency());
    }

    @Test
    void approvingAReturnWithoutAPaymentReferenceMarksItRefundedDirectly() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(orderReturnPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderReturn result = service.adminApprove(RETURN_ID, BigDecimal.valueOf(50), "VND", "wh-1");

        assertEquals(ReturnRefundStatus.REFUNDED, result.getRefundStatus());
        verify(paymentGateway, never()).refund(any(), any(), any(), any(), any());
    }

    @Test
    void aFailedRefundIsRecordedForRetry() {
        OrderReturn r = requestedReturn();
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(completedOrder()));
        when(orderReturnPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("gateway down")).when(paymentGateway)
                .refund(any(), any(), any(), any(), any());

        OrderReturn result = service.adminApprove(RETURN_ID, BigDecimal.valueOf(50), "VND", "wh-1");

        assertEquals(ReturnRefundStatus.FAILED, result.getRefundStatus());
        assertEquals(1, result.getRefundAttempts());
        assertEquals("gateway down", result.getRefundFailureReason());
    }

    @Test
    void aRefundInsideItsRetryWindowIsNotAttemptedAgain() {
        OrderReturn r = requestedReturn();
        r.approve(Money.of(BigDecimal.valueOf(50), "VND"), "wh-1",
                Instant.parse("2026-07-18T12:00:00Z"));
        r.markRefundFailed("gateway down", Instant.parse("2026-07-18T13:00:00Z"));
        when(orderReturnPersistencePort.findById(RETURN_ID)).thenReturn(Optional.of(r));

        OrderReturn result = service.retryRefund(RETURN_ID);

        assertEquals(ReturnRefundStatus.FAILED, result.getRefundStatus());
        verify(paymentGateway, never()).refund(any(), any(), any(), any(), any());
    }

    private static OrderReturn alreadyApproved(String currency, String warehouseId) {
        OrderReturn r = requestedReturn();
        r.approve(Money.of(BigDecimal.valueOf(100), currency), warehouseId,
                Instant.parse("2026-07-18T12:00:00Z"));
        return r;
    }
}

