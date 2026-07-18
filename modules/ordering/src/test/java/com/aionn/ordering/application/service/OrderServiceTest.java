package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.ordering.infrastructure.config.OrderingProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String USER_ID = "user-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String ORDER_ID = "order-1";

    @Mock private CartPersistencePort cartRepository;
    @Mock private OrderPersistencePort orderRepository;
    @Mock private OrderingResultMapper mapper;
    @Mock private EventPublisher eventPublisher;
    @Mock private StockReservationGateway stockReservationGateway;
    @Mock private PaymentGateway paymentGateway;
    @Mock private ShippingGateway shippingGateway;
    @Mock private CatalogPricingGateway catalogPricingGateway;
    @Mock private VoucherGateway voucherGateway;
    @Mock private CartService cartService;
    @Mock private MerchantQueryPort merchantQueryPort;
    @Mock private OrderingIntegrationEventPublisherPort integrationEventPublisher;
    @Mock private java.time.Clock clock;
    private final OrderingProperties orderingProperties = new OrderingProperties(
            new OrderingProperties.Reservation(86400),
            new OrderingProperties.AutoCancel(true, 15, 60_000L, 100));

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(java.time.Instant.parse("2026-07-18T12:00:00Z"));
        orderService = new OrderService(
                cartRepository, orderRepository, mapper, eventPublisher,
                stockReservationGateway, paymentGateway, shippingGateway,
                catalogPricingGateway, voucherGateway, cartService, merchantQueryPort,
                integrationEventPublisher, orderingProperties, clock);
    }

    private static ShippingAddress address() {
        return new ShippingAddress("addr-1", "User", "+84912345678",
                "12 main", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", 2, Money.of(BigDecimal.valueOf(100), "VND"),
                "wh-1", null);
    }

    private static Order pendingOrder() {
        Money subtotal = Money.of(BigDecimal.valueOf(200), "VND");
        Money shipping = Money.of(BigDecimal.ZERO, "VND");
        return Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1",
                "COD", "VND", List.of(item()), address(), shipping, subtotal, java.time.Instant.parse("2026-07-18T12:00:00Z"));
    }

    private static OrderResult sampleResult(String status) {
        return new OrderResult(ORDER_ID, null, USER_ID, MERCHANT_ID, "prop-1",
                "COD", null, "VND", BigDecimal.valueOf(200),
                BigDecimal.ZERO, "addr-1", "John", "12345", "line1",
                "ward", "dist", "prov", "VN", List.of(), status, null,
                java.time.Instant.now(), java.time.Instant.now(), null, null);
    }

    @Test
    void cancelMovesOrderToCancelledAndReleasesNothingWhenNoReservations() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(mapper.toResult(order)).thenReturn(sampleResult("CANCELLED"));

        OrderResult result = orderService.cancel(new CancelOrderCommand(ORDER_ID, USER_ID, "changed"));

        assertEquals("CANCELLED", result.status());
        verify(integrationEventPublisher).publishOrderCancelled(eq(ORDER_ID),
                eq("USER_CANCELLED"), eq("changed"),
                eq(OrderingIntegrationEventPublisherPort.CancellationKind.USER_CANCELLED));
    }

    @Test
    void cancelRejectsForeignUser() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(OrderingException.class,
                () -> orderService.cancel(new CancelOrderCommand(ORDER_ID, "other-user", "x")));
    }

    @Test
    void cancelOnUnknownOrderThrows() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.cancel(new CancelOrderCommand(ORDER_ID, USER_ID, "n")));
    }

    @Test
    void completeRequiresShippedStatus() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class,
                () -> orderService.complete(new ConfirmDeliveredCommand(ORDER_ID)));
        verify(integrationEventPublisher, never()).publishOrderCompleted(anyString());
    }

    @Test
    void completeOnShippedOrderPublishesCompletedEvent() {
        java.time.Instant now = java.time.Instant.parse("2026-07-18T12:00:00Z");
        Order order = pendingOrder();
        order.approve("p", now);
        order.confirmPreparation(now);
        order.markShipped("ship-1", now);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(mapper.toResult(order)).thenReturn(sampleResult("COMPLETED"));

        OrderResult result = orderService.complete(new ConfirmDeliveredCommand(ORDER_ID));

        assertEquals("COMPLETED", result.status());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(integrationEventPublisher).publishOrderCompleted(ORDER_ID);
    }

    @Test
    void rejectByMerchantRequiresOwnerToBeAMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("ghost")).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.rejectByMerchant(new RejectOrderCommand(ORDER_ID, "ghost", "x")));
    }

    @Test
    void rejectByMerchantSucceedsWhenOwnerOwnsTheMerchant() {
        Order order = pendingOrder();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1"))
                .thenReturn(Optional.of(MERCHANT_ID));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(mapper.toResult(order)).thenReturn(sampleResult("REJECTED"));

        OrderResult result = orderService.rejectByMerchant(
                new RejectOrderCommand(ORDER_ID, "owner-1", "no stock"));

        assertEquals("REJECTED", result.status());
        verify(integrationEventPublisher).publishOrderCancelled(eq(ORDER_ID),
                eq("MERCHANT_REJECTED"), eq("no stock"),
                eq(OrderingIntegrationEventPublisherPort.CancellationKind.MERCHANT_REJECTED));
    }

    @Test
    void getForRequesterReturnsResultWhenUserOwnsOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        OrderResult result = orderService.getForRequester(ORDER_ID, USER_ID);

        assertEquals("PENDING", result.status());
    }

    @Test
    void getForRequesterRejectsUnrelatedRequester() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder"))
                .thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.getForRequester(ORDER_ID, "intruder"));
    }

    @Test
    void listByUserMapsOrdersToResults() {
        Order order = pendingOrder();
        when(orderRepository.findByUser(USER_ID, 20)).thenReturn(List.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        List<OrderResult> results = orderService.listByUser(USER_ID, 20);

        assertEquals(1, results.size());
        assertEquals("PENDING", results.get(0).status());
    }

    @Test
    void markShippedCommitsReservations() {
        java.time.Instant now = java.time.Instant.parse("2026-07-18T12:00:00Z");
        Order order = pendingOrder();
        order.approve("p", now);
        order.confirmPreparation(now);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(mapper.toResult(order)).thenReturn(sampleResult("SHIPPED"));

        OrderResult result = orderService.markShipped(new ConfirmShippedCommand(ORDER_ID, "ship-1"));

        assertEquals("SHIPPED", result.status());
        verify(integrationEventPublisher).publishOrderShipped(ORDER_ID, "ship-1");
    }

    @Test
    void placeOrderHeadlessSucceedsWithValidParameters() {
        com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand command =
                new com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand(
                        USER_ID,
                        List.of(new com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand.Line("sku-1", 2)),
                        null, "COD", "VND", BigDecimal.ZERO, address()
                );

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(java.util.Map.of("sku-1", skuPricing));

        StockReservationGateway.Reservation reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 2, BigDecimal.valueOf(100), "VND"
        );
        when(stockReservationGateway.reserveAll(anyString(), org.mockito.ArgumentMatchers.anyList(), eq(86400)))
                .thenReturn(List.of(reservation));

        Order order = pendingOrder();
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(order);
        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
        when(cartService.loadOwned(USER_ID)).thenReturn(com.aionn.ordering.domain.model.Cart.create("cart-1", USER_ID, java.time.Instant.now()));
        when(mapper.toResult(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(sampleResult("APPROVED"));

        OrderResult result = orderService.placeOrderHeadless(command);

        assertEquals("APPROVED", result.status());
    }

    @Test
    void placeOrderHeadlessThrowsWhenSkuIsNotActive() {
        com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand command =
                new com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand(
                        USER_ID,
                        List.of(new com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand.Line("sku-1", 2)),
                        null, "COD", "VND", BigDecimal.ZERO, address()
                );

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", false
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(java.util.Map.of("sku-1", skuPricing));

        assertThrows(OrderingException.class, () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderSucceedsWhenCartIsValid() {
        com.aionn.ordering.application.dto.order.command.PlaceOrderCommand command =
                new com.aionn.ordering.application.dto.order.command.PlaceOrderCommand(
                        USER_ID, "addr-1", "COD", "VND", BigDecimal.ZERO, address(), List.of("sku-1"), "COD"
                );

        java.time.Instant now = java.time.Instant.now();
        com.aionn.ordering.domain.model.Cart cart = com.aionn.ordering.domain.model.Cart.create("cart-1", USER_ID, now);
        cart.addItem("sku-1", 2, now);
        when(cartService.loadOwned(USER_ID)).thenReturn(cart);

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(java.util.Map.of("sku-1", skuPricing));

        StockReservationGateway.Reservation reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 2, BigDecimal.valueOf(100), "VND"
        );
        when(stockReservationGateway.reserveAll(anyString(), org.mockito.ArgumentMatchers.anyList(), eq(86400)))
                .thenReturn(List.of(reservation));

        Order order = pendingOrder();
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(order);
        when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
        when(mapper.toResult(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(sampleResult("APPROVED"));

        OrderResult result = orderService.placeOrder(command);

        assertEquals("APPROVED", result.status());
    }

    @Test
    void approvePaymentSucceedsOnPendingOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(order);
        when(cartService.loadOwned(USER_ID)).thenReturn(com.aionn.ordering.domain.model.Cart.create("cart-1", USER_ID, java.time.Instant.now()));
        when(mapper.toResult(org.mockito.ArgumentMatchers.any(Order.class))).thenReturn(sampleResult("APPROVED"));

        OrderResult result = orderService.approvePayment(ORDER_ID, "pay-1");

        assertEquals("APPROVED", result.status());
        verify(integrationEventPublisher).publishOrderApproved(ORDER_ID, "pay-1");
    }

    @Test
    void getForRequesterReturnsOrderForOwner() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        OrderResult result = orderService.getForRequester(ORDER_ID, USER_ID);

        assertEquals("PENDING", result.status());
    }

    @Test
    void getForRequesterAllowsMerchantAccess() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        OrderResult result = orderService.getForRequester(ORDER_ID, "owner-1");

        assertEquals("PENDING", result.status());
    }

    @Test
    void getForRequesterThrowsForUnrelatedUser() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder")).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.getForRequester(ORDER_ID, "intruder"));
    }

    @Test
    void listByUserReturnsOrderList() {
        Order order = pendingOrder();
        when(orderRepository.findByUser(USER_ID, 20)).thenReturn(List.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        List<OrderResult> results = orderService.listByUser(USER_ID, 20);

        assertEquals(1, results.size());
    }

    @Test
    void listByUserWithStatusFilterReturnsFilteredOrders() {
        Order order = pendingOrder();
        when(orderRepository.findByUserAndStatuses(eq(USER_ID), org.mockito.ArgumentMatchers.anyList(), eq(20)))
                .thenReturn(List.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        List<OrderResult> results = orderService.listByUser(USER_ID, "PENDING", 20);

        assertEquals(1, results.size());
    }

    @Test
    void listByMerchantOwnerReturnsOrdersForMerchant() {
        Order order = pendingOrder();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));
        when(orderRepository.findByMerchant(MERCHANT_ID, 20)).thenReturn(List.of(order));
        when(mapper.toResult(order)).thenReturn(sampleResult("PENDING"));

        List<OrderResult> results = orderService.listByMerchantOwner("owner-1", null, 20);

        assertEquals(1, results.size());
    }

    @Test
    void cancelOnPaymentFailureCancelsWhenPending() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(order);
        when(mapper.toResult(order)).thenReturn(sampleResult("CANCELLED"));

        OrderResult result = orderService.cancelOnPaymentFailure(ORDER_ID, "ERR_001", "payment gateway error");

        assertEquals("CANCELLED", result.status());
    }

    @Test
    void statusOfReturnsPendingForNewOrder() {
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderStatus status = orderService.statusOf(ORDER_ID);

        assertEquals(OrderStatus.PENDING, status);
    }

    @Test
    void getMerchantAnalyticsReturnsCorrectResults() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));
        var start = java.time.LocalDate.now().minusDays(6);
        var to = java.time.LocalDate.now();
        var analyticsRows = List.of(
                new com.aionn.ordering.application.port.out.OrderPersistencePort.OrderAnalyticsRow(
                        "COMPLETED", java.math.BigDecimal.valueOf(100), "VND", java.time.Instant.now())
        );
        when(orderRepository.findMerchantAnalyticsRows(eq(MERCHANT_ID), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(analyticsRows);

        var result = orderService.getMerchantAnalytics("owner-1", start, to);

        assertEquals(1, result.totalOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(java.math.BigDecimal.valueOf(100), result.totalRevenue());
    }

    @Test
    void getPlatformAnalyticsReturnsCorrectResults() {
        var start = java.time.LocalDate.now().minusDays(5);
        var to = java.time.LocalDate.now();
        var platformRows = List.of(
                new com.aionn.ordering.application.port.out.OrderPersistencePort.PlatformAnalyticsRow(
                        MERCHANT_ID, "COMPLETED", java.math.BigDecimal.valueOf(200), "VND", java.time.Instant.now())
        );
        when(orderRepository.findPlatformAnalyticsRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(platformRows);

        var result = orderService.getPlatformAnalytics(start, to);

        assertEquals(1, result.totalOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(java.math.BigDecimal.valueOf(200), result.totalGmv());
    }
}
