package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.order.command.ChangeShippingInfoCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.domain.model.Cart;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.Map;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmPreparationCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.OrderPlacementOperationPort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.ordering.application.policy.OrderDefaultsPolicy;
import com.aionn.ordering.application.policy.OrderingAnalyticsPolicy;
import com.aionn.ordering.application.policy.ReservationPolicy;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.promotion.FlashSaleQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String USER_ID = "user-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String ORDER_ID = "order-1";

    @Mock private CartPersistencePort cartPersistencePort;
    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private EventPublisher eventPublisher;
    @Mock private StockReservationGateway stockReservationGateway;
    @Mock private PaymentGateway paymentGateway;
    @Mock private ShippingGateway shippingGateway;
    @Mock private CatalogPricingGateway catalogPricingGateway;
    @Mock private VoucherGateway voucherGateway;
    @Mock private CartService cartService;
    @Mock private MerchantQueryPort merchantQueryPort;
    @Mock private OrderingIntegrationEventPublisherPort integrationEventPublisher;
    @Mock private Clock clock;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock
    private ReservationPolicy reservationPolicy;
    @Mock private OrderDefaultsPolicy orderDefaultsPolicy;
    @Mock private OrderingAnalyticsPolicy analyticsPolicy;
    @Mock private CompensationTaskPort compensationTaskPort;
    @Mock private OrderPlacementOperationPort placementOperationPort;
    @Mock private FlashSaleQueryPort flashSaleQueryPort;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC);
        Mockito.lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        Mockito.lenient().when(clock.withZone(ArgumentMatchers.any())).thenReturn(fixedClock);
        Mockito.lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        Mockito.lenient().doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        orderService = new OrderService(
                cartPersistencePort, orderPersistencePort, eventPublisher,
                stockReservationGateway, paymentGateway, shippingGateway,
                catalogPricingGateway, voucherGateway, cartService, merchantQueryPort,
                integrationEventPublisher, reservationPolicy, orderDefaultsPolicy, analyticsPolicy,
                clock, transactionTemplate,
                compensationTaskPort, placementOperationPort, flashSaleQueryPort);
        lenient().when(placementOperationPort.start(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new OrderPlacementOperationPort.Operation(
                        invocation.getArgument(3), invocation.getArgument(2), false, true));
        lenient().when(reservationPolicy.ttlSeconds()).thenReturn(86400);
        lenient().when(orderDefaultsPolicy.defaultCurrency()).thenReturn("VND");
        lenient().when(analyticsPolicy.zone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        lenient().when(analyticsPolicy.merchantDefaultRangeDays()).thenReturn(7);
        lenient().when(analyticsPolicy.platformDefaultRangeDays()).thenReturn(30);
        lenient().when(analyticsPolicy.topMerchantsLimit()).thenReturn(10);
        lenient().when(analyticsPolicy.maxTopProductsLimit()).thenReturn(20);
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
                "COD", "VND", List.of(item()), address(), shipping, subtotal, Instant.parse("2026-07-18T12:00:00Z"));
    }

    @Test
    void cancelMovesOrderToCancelledAndReleasesNothingWhenNoReservations() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.cancel(new CancelOrderCommand(ORDER_ID, USER_ID, "changed"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(integrationEventPublisher).publishOrderCancelled(eq(ORDER_ID),
                eq("USER_CANCELLED"), eq("changed"),
                eq(OrderingIntegrationEventPublisherPort.CancellationKind.USER_CANCELLED));
    }

    @Test
    void cancelRejectsForeignUser() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(OrderingException.class,
                () -> orderService.cancel(new CancelOrderCommand(ORDER_ID, "other-user", "x")));
    }

    @Test
    void cancelOnUnknownOrderThrows() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.cancel(new CancelOrderCommand(ORDER_ID, USER_ID, "n")));
    }

    @Test
    void completeRequiresShippedStatus() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderingException.class,
                () -> orderService.complete(new ConfirmDeliveredCommand(ORDER_ID)));
        verify(integrationEventPublisher, never()).publishOrderCompleted(anyString());
    }

    @Test
    void completeOnShippedOrderPublishesCompletedEvent() {
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        Order order = pendingOrder();
        order.approve("p", now);
        order.confirmPreparation(now);
        order.markShipped("ship-1", now);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.complete(new ConfirmDeliveredCommand(ORDER_ID));

        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(integrationEventPublisher).publishOrderCompleted(ORDER_ID);
    }

    @Test
    void rejectByMerchantRequiresMerchantId() {
        // Command now carries the verified merchantId from @CurrentMerchantId;
        // a blank one is rejected before any repository lookup.
        assertThrows(OrderingException.class,
                () -> orderService.rejectByMerchant(new RejectOrderCommand(ORDER_ID, " ", "x")));
    }

    @Test
    void rejectByMerchantSucceedsWhenOwnerOwnsTheMerchant() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.rejectByMerchant(
                new RejectOrderCommand(ORDER_ID, MERCHANT_ID, "no stock"));

        assertEquals(OrderStatus.REJECTED, result.getStatus());
        verify(integrationEventPublisher).publishOrderCancelled(eq(ORDER_ID),
                eq("MERCHANT_REJECTED"), eq("no stock"),
                eq(OrderingIntegrationEventPublisherPort.CancellationKind.MERCHANT_REJECTED));
    }

    @Test
    void getForRequesterReturnsResultWhenUserOwnsOrder() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = orderService.getForRequester(ORDER_ID, USER_ID);

        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void getForRequesterRejectsUnrelatedRequester() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder"))
                .thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.getForRequester(ORDER_ID, "intruder"));
    }

    @Test
    void listByUserMapsOrdersToResults() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByUser(USER_ID, 20)).thenReturn(List.of(order));

        List<Order> results = orderService.listByUser(USER_ID, 20);

        assertEquals(1, results.size());
        assertEquals(OrderStatus.PENDING, results.get(0).getStatus());
    }

    @Test
    void markShippedCommitsReservations() {
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        Order order = pendingOrder();
        order.approve("p", now);
        order.confirmPreparation(now);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.markShipped(new ConfirmShippedCommand(ORDER_ID, "ship-1"));

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        verify(integrationEventPublisher).publishOrderShipped(ORDER_ID, "ship-1");
    }

    @Test
    void placeOrderHeadlessSucceedsWithValidParameters() {
        PlaceOrderHeadlessCommand command =
                new PlaceOrderHeadlessCommand(
                        USER_ID,
                        List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 2)),
                        null, "COD", "VND", address()
                );

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of(), "registration-1",
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null)
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));

        StockReservationGateway.Reservation reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 2, BigDecimal.valueOf(100), "VND"
        );
        when(stockReservationGateway.reserveAll(anyString(), ArgumentMatchers.anyList(), eq(86400)))
                .thenReturn(List.of(reservation));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(30000), "VND"));

        when(orderPersistencePort.save(ArgumentMatchers.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.placeOrderHeadless(command);

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        assertEquals(BigDecimal.valueOf(30000), result.getShippingFee().amount());
        verify(flashSaleQueryPort).reserve(anyString(), eq(List.of(
                new FlashSaleQueryPort.Allocation("registration-1", 2))));
    }

    @Test
    void reservationFailurePersistsFlashSaleReleaseWhenImmediateReleaseFails() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line(
                        "sku-1", 2)),
                null, "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of(), "registration-1",
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenThrow(new StockReservationGateway.ReservationException("sku-1", "out of stock"));
        doThrow(new RuntimeException("promotion unavailable"))
                .when(flashSaleQueryPort).release(anyString());

        assertThrows(OrderingException.class, () -> orderService.placeOrderHeadless(command));

        ArgumentCaptor<CompensationTaskPort.Task> taskCaptor =
                ArgumentCaptor.forClass(CompensationTaskPort.Task.class);
        verify(compensationTaskPort).enqueue(taskCaptor.capture());
        assertEquals(CompensationTaskPort.Type.FLASH_SALE_RELEASE, taskCaptor.getValue().type());
        assertEquals(taskCaptor.getValue().orderId(), taskCaptor.getValue().resourceId());
    }

    @Test
    void placeOrderHeadlessThrowsWhenSkuIsNotActive() {
        PlaceOrderHeadlessCommand command =
                new PlaceOrderHeadlessCommand(
                        USER_ID,
                        List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 2)),
                        null, "COD", "VND", address()
                );

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", false
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));

        assertThrows(OrderingException.class, () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderSucceedsWhenCartIsValid() {
        PlaceOrderCommand command =
                new PlaceOrderCommand(
                        USER_ID, "addr-1", "COD", "VND", address(), List.of("sku-1"), "COD"
                );

        Instant now = Instant.now();
        Cart cart = Cart.create("cart-1", USER_ID, now);
        cart.addItem("sku-1", 2, now);
        when(cartService.loadOwned(USER_ID)).thenReturn(cart);

        CatalogPricingGateway.SkuPricing skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of(), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null)
        );
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));

        StockReservationGateway.Reservation reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 2, BigDecimal.valueOf(100), "VND"
        );
        when(stockReservationGateway.reserveAll(anyString(), ArgumentMatchers.anyList(), eq(86400)))
                .thenReturn(List.of(reservation));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(25000), "VND"));

        when(orderPersistencePort.save(ArgumentMatchers.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.placeOrder(command);

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        assertEquals(BigDecimal.valueOf(25000), result.getShippingFee().amount());
    }

    @Test
    void placementQuotesShippingBeforeReservingStock() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of("category-1"), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenThrow(new RuntimeException("carrier unavailable"));

        assertThrows(RuntimeException.class, () -> orderService.placeOrderHeadless(command));

        verify(stockReservationGateway, never()).reserveAll(anyString(), any(), eq(86400));
        verify(orderPersistencePort, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("invalidShippingQuotes")
    void placementRejectsInvalidShippingQuoteBeforeSideEffects(ShippingGateway.ShippingQuote quote) {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of(), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(quote);

        assertThrows(OrderingException.class, () -> orderService.placeOrderHeadless(command));

        verify(stockReservationGateway, never()).reserveAll(anyString(), any(), eq(86400));
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void placementRejectsMultiMerchantCartWithDedicatedErrorBeforeSideEffects() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(
                        new PlaceOrderHeadlessCommand.Line("sku-1", 1),
                        new PlaceOrderHeadlessCommand.Line("sku-2", 1)),
                null, "COD", "VND", address());
        var firstMerchantSku = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true);
        var secondMerchantSku = new CatalogPricingGateway.SkuPricing(
                "sku-2", "merchant-2", "wh-2", BigDecimal.valueOf(200), "VND", true);
        when(catalogPricingGateway.resolve(List.of("sku-1", "sku-2")))
                .thenReturn(Map.of("sku-1", firstMerchantSku, "sku-2", secondMerchantSku));

        assertThatThrownBy(() -> orderService.placeOrderHeadless(command))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.ORDER_MULTI_MERCHANT_NOT_SUPPORTED.getCode());

        verify(shippingGateway, never()).quote(any(), any(), any(), any(), anyList(), any());
        verify(stockReservationGateway, never()).reserveAll(anyString(), any(), any(Integer.class));
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void completedPlacementReplayReturnsExistingOrderWithoutRepeatingSideEffects() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address(), "client-order-1");
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true);
        Order existing = pendingOrder();
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(placementOperationPort.start(eq(USER_ID), eq("client-order-1"), anyString(), anyString()))
                .thenAnswer(invocation -> new OrderPlacementOperationPort.Operation(
                        ORDER_ID, invocation.getArgument(2), true, true));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(existing));

        Order replay = orderService.placeOrderHeadless(command);

        assertEquals(ORDER_ID, replay.getOrderId());
        verify(shippingGateway, never()).quote(any(), any(), any(), any(), anyList(), any());
        verify(stockReservationGateway, never()).reserveAll(any(), any(), any(Integer.class));
        verify(placementOperationPort).complete(USER_ID, "client-order-1", ORDER_ID);
    }

    private static Stream<Arguments> invalidShippingQuotes() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(new ShippingGateway.ShippingQuote(null, "VND")),
                Arguments.of(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(-1), "VND")),
                Arguments.of(new ShippingGateway.ShippingQuote(BigDecimal.ONE, "USD")),
                Arguments.of(new ShippingGateway.ShippingQuote(BigDecimal.ONE, null)));
    }

    @Test
    void placementCompensatesVoucherAndStockWhenOrderPersistenceFails() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "SAVE10", "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of(), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        var reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND");
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), any(), eq(86400))).thenReturn(List.of(reservation));
        when(voucherGateway.apply(eq(USER_ID), eq(MERCHANT_ID), eq("SAVE10"), anyString(),
                eq(BigDecimal.valueOf(100)), eq("VND"), eq(List.of("category-1"))))
                .thenReturn(new VoucherGateway.Discount(BigDecimal.TEN, "VND", true, null));
        when(orderPersistencePort.save(any(Order.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class, () -> orderService.placeOrderHeadless(command));

        // The voucher writes joined the rolled-back order transaction, so it must
        // not be released a second time by the external compensation path.
        verify(voucherGateway, never()).release(anyString(), anyString(), anyString());
        verify(stockReservationGateway).release("res-1", "order-placement-failed");
    }

    @Test
    void placementPersistsEachCompensationThatCannotCompleteImmediately() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "SAVE10", "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                List.of("category-1"), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        var reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND");
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), any(), eq(86400))).thenReturn(List.of(reservation));
        when(voucherGateway.apply(eq(USER_ID), eq(MERCHANT_ID), eq("SAVE10"), anyString(),
                eq(BigDecimal.valueOf(100)), eq("VND"), eq(List.of("category-1"))))
                .thenReturn(new VoucherGateway.Discount(BigDecimal.TEN, "VND", true, null));
        when(orderPersistencePort.save(any(Order.class))).thenThrow(new RuntimeException("database unavailable"));
        doThrow(new RuntimeException("inventory unavailable"))
                .when(stockReservationGateway).release("res-1", "order-placement-failed");

        assertThrows(RuntimeException.class, () -> orderService.placeOrderHeadless(command));

        verify(compensationTaskPort).enqueue(any(CompensationTaskPort.Task.class));
    }

    @Test
    void placementRejectsVoucherWithDifferentCurrencyAndReleasesStock() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "SAVE10", "COD", "USD", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "USD", true,
                List.of(), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        var reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "USD");
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("USD"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "USD"));
        when(stockReservationGateway.reserveAll(anyString(), any(), eq(86400))).thenReturn(List.of(reservation));
        when(voucherGateway.apply(eq(USER_ID), eq(MERCHANT_ID), eq("SAVE10"), anyString(),
                any(BigDecimal.class), eq("USD"), anyList()))
                .thenReturn(new VoucherGateway.Discount(BigDecimal.valueOf(100_000), "VND", true, null));

        assertThatThrownBy(() -> orderService.placeOrderHeadless(command))
                .isInstanceOf(OrderingException.class)
                .hasMessageContaining("currency");

        verify(voucherGateway, never()).release(anyString(), anyString(), anyString());
        verify(stockReservationGateway).release("res-1", "order-placement-failed");
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void freeShippingVoucherWaivesTheFeeWithoutTouchingGoodsSubtotal() {
        var command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "FREESHIP30K", "COD", "VND", address());
        var skuPricing = new CatalogPricingGateway.SkuPricing(
                "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(200_000), "VND", true,
                List.of(), null,
                new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null));
        var reservation = new StockReservationGateway.Reservation(
                "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(200_000), "VND");
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1", skuPricing));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(35_000), "VND"));
        when(stockReservationGateway.reserveAll(anyString(), any(), eq(86400))).thenReturn(List.of(reservation));
        when(voucherGateway.apply(eq(USER_ID), eq(MERCHANT_ID), eq("FREESHIP30K"), anyString(),
                any(BigDecimal.class), eq("VND"), anyList()))
                .thenReturn(new VoucherGateway.Discount(BigDecimal.valueOf(30_000), "VND", true, null, true));

        AtomicReference<Order> saved =
                new AtomicReference<>();
        when(orderPersistencePort.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            saved.set(o);
            return o;
        });

        orderService.placeOrderHeadless(command);

        Order order = saved.get();
        // 35k fee waived by at most the voucher's 30k -> customer still pays 5k.
        assertEquals(BigDecimal.valueOf(5_000), order.getShippingFee().amount());
        // Goods subtotal is untouched by a shipping voucher.
        assertEquals(0, order.getTotalAmount().amount()
                .compareTo(BigDecimal.valueOf(205_000)));
    }

    @Test
    void changeShippingInfoAlwaysUsesCarrierQuote() {
        Order order = pendingOrder();
        ShippingAddress replacement = new ShippingAddress("addr-2", "User", "+84912345678",
                "99 new street", "WARD-2", "DIST-2", "PROV-2", "VN");
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(catalogPricingGateway.resolve(List.of("sku-1"))).thenReturn(Map.of("sku-1",
                new CatalogPricingGateway.SkuPricing(
                        "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true,
                        List.of(), null,
                        new CatalogPricingGateway.SkuPricing.Parcel(500, null, null, null))));
        when(shippingGateway.quote(eq(ORDER_ID), eq(MERCHANT_ID), eq(replacement), eq("VND"), anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(45000), "VND"));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.changeShippingInfo(
                new ChangeShippingInfoCommand(
                        ORDER_ID, USER_ID, replacement));

        assertEquals(BigDecimal.valueOf(45000), result.getShippingFee().amount());
        assertEquals(replacement, result.getShippingAddress());
    }

    @Test
    void approvePaymentSucceedsOnPendingOrder() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(ArgumentMatchers.any(Order.class))).thenReturn(order);
        when(cartService.loadOwned(USER_ID)).thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));

        Order result = orderService.approvePayment(ORDER_ID, "pay-1");

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        verify(integrationEventPublisher).publishOrderApproved(ORDER_ID, "pay-1");
    }

    @Test
    void getForRequesterReturnsOrderForOwner() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = orderService.getForRequester(ORDER_ID, USER_ID);

        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void getForRequesterAllowsMerchantAccess() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of(MERCHANT_ID));

        Order result = orderService.getForRequester(ORDER_ID, "owner-1");

        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void getForRequesterThrowsForUnrelatedUser() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(merchantQueryPort.findMerchantIdByOwnerId("intruder")).thenReturn(Optional.empty());

        assertThrows(OrderingException.class,
                () -> orderService.getForRequester(ORDER_ID, "intruder"));
    }

    @Test
    void listByUserReturnsOrderList() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByUser(USER_ID, 20)).thenReturn(List.of(order));

        List<Order> results = orderService.listByUser(USER_ID, 20);

        assertEquals(1, results.size());
    }

    @Test
    void listByUserWithStatusFilterReturnsFilteredOrders() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByUserAndStatuses(eq(USER_ID), ArgumentMatchers.anyList(), eq(20)))
                .thenReturn(List.of(order));

        List<Order> results = orderService.listByUser(USER_ID, "PENDING", 20);

        assertEquals(1, results.size());
    }

    @Test
    void listByMerchantOwnerReturnsOrdersForMerchant() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByMerchant(MERCHANT_ID, 20)).thenReturn(List.of(order));

        List<Order> results = orderService.listByMerchantOwner(MERCHANT_ID, null, 20);

        assertEquals(1, results.size());
    }

    @Test
    void cancelOnPaymentFailureCancelsWhenPending() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(ArgumentMatchers.any())).thenReturn(order);

        Order result = orderService.cancelOnPaymentFailure(ORDER_ID, "ERR_001", "payment gateway error");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void statusOfReturnsPendingForNewOrder() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderStatus status = orderService.statusOf(ORDER_ID);

        assertEquals(OrderStatus.PENDING, status);
    }

    @Test
    void listByMerchantOwnerThrowsWhenMerchantIdBlank() {
        assertThrows(OrderingException.class, () -> orderService.listByMerchantOwner(" ", null, 10));
    }

    @Test
    void getMerchantAnalyticsThrowsWhenMerchantIdBlank() {
        assertThrows(OrderingException.class, () -> orderService.getMerchantAnalytics(" ", null, null));
    }

    @Test
    void getMerchantAnalyticsReturnsCorrectResults() {
        var start = LocalDate.now().minusDays(6);
        var to = LocalDate.now();
        var analyticsRows = List.of(
                new OrderPersistencePort.OrderAnalyticsRow(
                        "COMPLETED", BigDecimal.valueOf(100), "VND", 
                        to.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        when(orderPersistencePort.findMerchantAnalyticsRows(eq(MERCHANT_ID), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(analyticsRows);

        var result = orderService.getMerchantAnalytics(MERCHANT_ID, start, to);

        assertEquals(1, result.totalOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(BigDecimal.valueOf(100), result.totalRevenue());
    }

    @Test
    void getPlatformAnalyticsReturnsCorrectResults() {
        var start = LocalDate.now().minusDays(5);
        var to = LocalDate.now();
        var platformRows = List.of(
                new OrderPersistencePort.PlatformAnalyticsRow(
                        MERCHANT_ID, "COMPLETED", BigDecimal.valueOf(200), "VND", 
                        to.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
        );
        when(orderPersistencePort.findPlatformAnalyticsRows(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(platformRows);

        var result = orderService.getPlatformAnalytics(start, to);

        assertEquals(1, result.totalOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(BigDecimal.valueOf(200), result.totalGmv());
    }

    @Test
    void getMerchantTopProductsUsesClockForDefaultDateRange() {
        when(orderPersistencePort.findMerchantTopProductRows(
                eq(MERCHANT_ID), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(List.of(new OrderPersistencePort.TopProductRow(
                        "sku-1", 3, BigDecimal.valueOf(300))));

        var result = orderService.getMerchantTopProducts(MERCHANT_ID, null, null, 5);

        assertEquals(1, result.size());
        assertEquals("sku-1", result.getFirst().skuId());
    }

    @Test
    void statusOfFallsBackToOrderSnWhenNotFoundById() {
        Order order = pendingOrder();
        String orderSn = order.getOrderSn();
        when(orderPersistencePort.findById(orderSn)).thenReturn(Optional.empty());
        when(orderPersistencePort.findByIdOrOrderSn(orderSn)).thenReturn(Optional.of(order));

        OrderStatus status = orderService.statusOf(orderSn);

        assertEquals(OrderStatus.PENDING, status);
    }

    // ------------------------------------------------------------------
    // Helpers for the additional branch-coverage tests.
    // ------------------------------------------------------------------

    private static void assertOrderingError(OrderingErrorCode expected,
            org.junit.jupiter.api.function.Executable executable) {
        assertThatThrownBy(executable::execute)
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(expected.getCode());
    }

    private static CatalogPricingGateway.SkuPricing pricedSku(String skuId, String merchantId,
            String currency, BigDecimal price, Integer weightGram, String flashSaleRegistrationId) {
        return new CatalogPricingGateway.SkuPricing(skuId, merchantId, "wh-1", price, currency, true,
                List.of(), flashSaleRegistrationId,
                new CatalogPricingGateway.SkuPricing.Parcel(weightGram, null, null, null));
    }

    private static CatalogPricingGateway.SkuPricing pricedSku(String skuId) {
        return pricedSku(skuId, MERCHANT_ID, "VND", BigDecimal.valueOf(100), 500, null);
    }

    private static Order orderWithReservation() {
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        OrderItem reserved = new OrderItem("sku-1", 1, Money.of(BigDecimal.valueOf(100), "VND"),
                "wh-1", "res-1");
        return Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1", "COD", "VND",
                List.of(reserved), address(), Money.zero("VND"),
                Money.of(BigDecimal.valueOf(100), "VND"), now);
    }

    private static Order approvedReservedOrder() {
        Order order = orderWithReservation();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        return order;
    }

    private static Order reservedOrderInPreparation() {
        Order order = approvedReservedOrder();
        order.confirmPreparation(Instant.parse("2026-07-18T12:00:00Z"));
        return order;
    }

    // ------------------------------------------------------------------
    // placeOrder / payment-selection guards
    // ------------------------------------------------------------------

    @Test
    void placeOrderRejectsExternalGatewayWithoutPaymentMethod() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", null, "VND", address(), List.of("sku-1"), "VNPAY");

        assertOrderingError(OrderingErrorCode.ORDER_PAYMENT_FAILED,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderRejectsBlankPaymentMethodForExternalGateway() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "  ", "VND", address(), List.of("sku-1"), "VNPAY");

        assertOrderingError(OrderingErrorCode.ORDER_PAYMENT_FAILED,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderRejectsCodGatewayWithNonCodPaymentMethod() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "CARD", "VND", address(), List.of("sku-1"), "COD");

        assertOrderingError(OrderingErrorCode.ORDER_PAYMENT_FAILED,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderTreatsNullGatewayAsCod() {
        when(cartService.loadOwned(USER_ID))
                .thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "COD", "VND", address(), List.of("sku-1"), null);

        assertOrderingError(OrderingErrorCode.CART_EMPTY,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderRejectsEmptyCart() {
        when(cartService.loadOwned(USER_ID))
                .thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "COD", "VND", address(), List.of("sku-1"), "COD");

        assertOrderingError(OrderingErrorCode.CART_EMPTY,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderRejectsSelectionThatNoLongerExistsInCart() {
        Instant now = Instant.now();
        Cart cart = Cart.create("cart-1", USER_ID, now);
        cart.addItem("sku-1", 2, now);
        when(cartService.loadOwned(USER_ID)).thenReturn(cart);
        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "COD", "VND", address(), List.of("sku-2"), "COD");

        assertOrderingError(OrderingErrorCode.CART_ITEM_NOT_FOUND,
                () -> orderService.placeOrder(command));
    }

    @Test
    void placeOrderWithExplicitSelectionPlacesOnlySelectedLines() {
        Instant now = Instant.now();
        Cart cart = Cart.create("cart-1", USER_ID, now);
        cart.addItem("sku-1", 2, now);
        cart.addItem("sku-2", 1, now);
        when(cartService.loadOwned(USER_ID)).thenReturn(cart);

        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(25000), "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 2, BigDecimal.valueOf(100), "VND")));
        when(orderPersistencePort.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlaceOrderCommand command = new PlaceOrderCommand(
                USER_ID, "addr-1", "COD", "VND", address(), List.of("sku-1"), "COD");

        Order result = orderService.placeOrder(command);

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("sku-1", result.getItems().getFirst().skuId());
        // The unselected line stays in the cart.
        assertEquals(1, cart.snapshot().size());
        assertEquals("sku-2", cart.snapshot().getFirst().getKey());
    }

    // ------------------------------------------------------------------
    // placeOrderHeadless / placeFromLines guards
    // ------------------------------------------------------------------

    @Test
    void placeOrderHeadlessRejectsNullLines() {
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID, null, null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.CART_EMPTY,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsEmptyLines() {
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID, List.of(), null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.CART_EMPTY,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsBlankSku() {
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("  ", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsNonPositiveQuantity() {
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 0)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsDuplicateSku() {
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1),
                        new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsMoreThanHundredLines() {
        List<PlaceOrderHeadlessCommand.Line> lines = java.util.stream.IntStream
                .range(0, 101)
                .mapToObj(i -> new PlaceOrderHeadlessCommand.Line("sku-" + i, 1))
                .toList();
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID, lines, null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsSkuMissingFromPricing() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of());
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsSkuWithoutPrice() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku(
                        "sku-1", MERCHANT_ID, "VND", null, 500, null)));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsMixedCurrencies() {
        when(catalogPricingGateway.resolve(List.of("sku-1", "sku-2")))
                .thenReturn(Map.of(
                        "sku-1", pricedSku("sku-1", MERCHANT_ID, "VND",
                                BigDecimal.valueOf(100), 500, null),
                        "sku-2", pricedSku("sku-2", MERCHANT_ID, "USD",
                                BigDecimal.valueOf(100), 500, null)));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1),
                        new PlaceOrderHeadlessCommand.Line("sku-2", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsRequestCurrencyMismatch() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "USD", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsSkuWithoutParcel() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", new CatalogPricingGateway.SkuPricing(
                        "sku-1", MERCHANT_ID, "wh-1", BigDecimal.valueOf(100), "VND", true)));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsSkuWithZeroWeight() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku(
                        "sku-1", MERCHANT_ID, "VND", BigDecimal.valueOf(100), 0, null)));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));
    }

    @Test
    void placeOrderHeadlessRejectsWhenAnotherPlacementHoldsTheIdempotencyKey() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(placementOperationPort.start(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new OrderPlacementOperationPort.Operation(
                        "order-other", invocation.getArgument(2), false, false));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_PLACEMENT_IN_PROGRESS,
                () -> orderService.placeOrderHeadless(command));

        verify(shippingGateway, never()).quote(any(), any(), any(), any(), anyList(), any());
        verify(stockReservationGateway, never()).reserveAll(anyString(), any(), any(Integer.class));
    }

    @Test
    void placeOrderHeadlessRejectsWhenFlashSaleCapacityIsExceeded() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku(
                        "sku-1", MERCHANT_ID, "VND", BigDecimal.valueOf(100), 500, "reg-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        doThrow(new FlashSaleQueryPort.CapacityExceededException("reg-1"))
                .when(flashSaleQueryPort).reserve(anyString(), anyList());
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_RESERVATION_FAILED,
                () -> orderService.placeOrderHeadless(command));

        verify(stockReservationGateway, never()).reserveAll(anyString(), any(), any(Integer.class));
    }

    @Test
    void placeOrderHeadlessRethrowsUnexpectedReservationFailure() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenThrow(new IllegalStateException("inventory unavailable"));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address());

        assertThrows(IllegalStateException.class,
                () -> orderService.placeOrderHeadless(command));

        verify(flashSaleQueryPort).release(anyString());
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void placeOrderHeadlessRejectsInvalidVoucher() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND")));
        when(voucherGateway.apply(anyString(), anyString(), eq("EXPIRED"), anyString(),
                any(BigDecimal.class), eq("VND"), anyList()))
                .thenReturn(new VoucherGateway.Discount(
                        BigDecimal.ZERO, "VND", false, "voucher expired"));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "EXPIRED", "COD", "VND", address());

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.placeOrderHeadless(command));

        verify(stockReservationGateway).release("res-1", "order-placement-failed");
        verify(orderPersistencePort, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("invalidVoucherDiscountAmounts")
    void placeOrderHeadlessRejectsInvalidVoucherDiscountAmount(BigDecimal amount) {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND")));
        when(voucherGateway.apply(anyString(), anyString(), eq("SAVE10"), anyString(),
                any(BigDecimal.class), eq("VND"), anyList()))
                .thenReturn(new VoucherGateway.Discount(amount, "VND", true, null));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "SAVE10", "COD", "VND", address());

        assertThatThrownBy(() -> orderService.placeOrderHeadless(command))
                .isInstanceOf(OrderingException.class)
                .hasMessageContaining("invalid discount amount");

        verify(stockReservationGateway).release("res-1", "order-placement-failed");
    }

    private static Stream<Arguments> invalidVoucherDiscountAmounts() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(BigDecimal.valueOf(-1)));
    }

    @Test
    void placeOrderHeadlessAppliesNonShippingVoucherDiscount() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.valueOf(10), "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND")));
        when(voucherGateway.apply(anyString(), anyString(), eq("SAVE10"), anyString(),
                any(BigDecimal.class), eq("VND"), anyList()))
                .thenReturn(new VoucherGateway.Discount(
                        BigDecimal.TEN, "VND", true, null));
        AtomicReference<Order> saved = new AtomicReference<>();
        when(orderPersistencePort.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            saved.set(order);
            return order;
        });
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                "SAVE10", "COD", "VND", address());

        orderService.placeOrderHeadless(command);

        assertEquals(0, saved.get().getTotalAmount().amount()
                .compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, saved.get().getShippingFee().amount()
                .compareTo(BigDecimal.TEN));
    }

    @Test
    void placeOrderHeadlessLeavesOrderPendingForExternalPayment() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any()))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND")));
        when(orderPersistencePort.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "CARD", "VND", address());

        Order result = orderService.placeOrderHeadless(command);

        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(integrationEventPublisher, never()).publishOrderApproved(anyString(), anyString());
    }

    @Test
    void placeOrderHeadlessUsesServiceQuoteLegacyKeyAndAttachesMetadata() {
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        when(shippingGateway.quote(anyString(), eq(MERCHANT_ID), eq(address()), eq("VND"),
                anyList(), any(), eq("EXPRESS")))
                .thenReturn(new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND"));
        when(stockReservationGateway.reserveAll(anyString(), anyList(), eq(86400)))
                .thenReturn(List.of(new StockReservationGateway.Reservation(
                        "res-1", "sku-1", "wh-1", 1, BigDecimal.valueOf(100), "VND")));
        when(orderPersistencePort.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlaceOrderHeadlessCommand command = new PlaceOrderHeadlessCommand(
                USER_ID,
                List.of(new PlaceOrderHeadlessCommand.Line("sku-1", 1)),
                null, "COD", "VND", address(),
                "   ", "leave at door", "EXPRESS");

        Order result = orderService.placeOrderHeadless(command);

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        assertEquals("leave at door", result.getNote());
        assertEquals("EXPRESS", result.getShippingService());
        verify(placementOperationPort).start(eq(USER_ID),
                ArgumentMatchers.startsWith("legacy:"), anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // approvePayment branches
    // ------------------------------------------------------------------

    @Test
    void approvePaymentThrowsWhenOrderIsMissing() {
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());

        assertOrderingError(OrderingErrorCode.ORDER_NOT_FOUND,
                () -> orderService.approvePayment(ORDER_ID, "pay-1"));
    }

    @Test
    void approvePaymentIsIdempotentWhenSamePaymentAlreadyRecorded() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(cartService.loadOwned(USER_ID))
                .thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));

        Order result = orderService.approvePayment(ORDER_ID, "pay-1");

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        verify(orderPersistencePort, never()).save(any());
        verify(paymentGateway, never()).refund(anyString(), any(), anyString(), anyString(),
                anyString());
        verify(integrationEventPublisher, never()).publishOrderApproved(anyString(), anyString());
    }

    @Test
    void approvePaymentRefundsDuplicateCaptureWithDifferentPaymentId() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(cartService.loadOwned(USER_ID))
                .thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));

        Order result = orderService.approvePayment(ORDER_ID, "pay-2");

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        verify(paymentGateway).refund(eq("pay-2"), eq(BigDecimal.valueOf(200)), eq("VND"),
                eq("duplicate payment capture"), eq("order:order-1:refund:pay-2"));
        verify(compensationTaskPort).markCompleted("refund:order-1:pay-2");
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void approvePaymentRefundsCapturedAmountWhenOrderWasCancelled() {
        Order order = pendingOrder();
        order.cancel("USER_CANCELLED", "changed mind",
                Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(cartService.loadOwned(USER_ID))
                .thenReturn(Cart.create("cart-1", USER_ID, Instant.now()));

        Order result = orderService.approvePayment(ORDER_ID, "pay-9",
                BigDecimal.valueOf(150), "VND");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(paymentGateway).refund(eq("pay-9"), eq(BigDecimal.valueOf(150)), eq("VND"),
                eq("payment captured after cancellation"),
                eq("order:order-1:refund:pay-9"));
        verify(orderPersistencePort, never()).save(any());
        verify(integrationEventPublisher, never()).publishOrderApproved(anyString(), anyString());
    }

    @Test
    void approvePaymentRemovesVoucherWhenItemsRemainInCart() {
        Instant now = Instant.now();
        Order order = pendingOrder();
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = Cart.create("cart-1", USER_ID, now);
        cart.addItem("sku-1", 2, now);
        cart.addItem("sku-2", 1, now);
        cart.applyVoucher("SAVE10", now);
        when(cartService.loadOwned(USER_ID)).thenReturn(cart);

        orderService.approvePayment(ORDER_ID, "pay-1");

        assertEquals(1, cart.snapshot().size());
        assertEquals("sku-2", cart.snapshot().getFirst().getKey());
        assertNull(cart.getVoucherCode());
        verify(cartPersistencePort).save(cart);
    }

    @Test
    void cancelToleratesCompensationCompletionFailure() {
        Order order = approvedReservedOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("task store down"))
                .when(compensationTaskPort).markCompleted(anyString());

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(paymentGateway).refund(eq("pay-1"), eq(BigDecimal.valueOf(100)), eq("VND"),
                eq("user cancellation"), eq("order:order-1:refund:pay-1"));
    }

    // ------------------------------------------------------------------
    // confirmPreparation / shipment registration
    // ------------------------------------------------------------------

    @Test
    void confirmPreparationRegistersShipment() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));

        Order result = orderService.confirmPreparation(
                new ConfirmPreparationCommand(ORDER_ID, MERCHANT_ID));

        assertEquals(OrderStatus.PREPARING, result.getStatus());
        verify(shippingGateway).createAndRegister(eq(ORDER_ID), eq(MERCHANT_ID), eq(USER_ID),
                eq(address()), ArgumentMatchers.isNull(), eq(BigDecimal.ZERO), eq("VND"),
                anyList());
    }

    @Test
    void confirmPreparationSwallowsShipmentRegistrationFailure() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));
        doThrow(new RuntimeException("carrier unavailable"))
                .when(shippingGateway).createAndRegister(anyString(), anyString(), anyString(),
                        any(), any(), any(), anyString(), anyList());

        Order result = orderService.confirmPreparation(
                new ConfirmPreparationCommand(ORDER_ID, MERCHANT_ID));

        assertEquals(OrderStatus.PREPARING, result.getStatus());
    }

    @Test
    void confirmPreparationPassesNullFeeWhenOrderHasNoShippingFee() {
        Order order = Order.place(ORDER_ID, USER_ID, MERCHANT_ID, "prop-1", "COD", "VND",
                List.of(item()), address(), null,
                Money.of(BigDecimal.valueOf(200), "VND"),
                Instant.parse("2026-07-18T12:00:00Z"));
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        when(catalogPricingGateway.resolve(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricedSku("sku-1")));

        Order result = orderService.confirmPreparation(
                new ConfirmPreparationCommand(ORDER_ID, MERCHANT_ID));

        assertEquals(OrderStatus.PREPARING, result.getStatus());
        verify(shippingGateway).createAndRegister(eq(ORDER_ID), eq(MERCHANT_ID), eq(USER_ID),
                eq(address()), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                eq("VND"), anyList());
    }

    @Test
    void confirmPreparationRejectsOrderOwnedByAnotherMerchant() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertOrderingError(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                () -> orderService.confirmPreparation(
                        new ConfirmPreparationCommand(ORDER_ID, "merchant-2")));
    }

    // ------------------------------------------------------------------
    // cancel / cancelOnPaymentFailure / rejectByMerchant
    // ------------------------------------------------------------------

    @Test
    void cancelRejectsOrderAlreadyPickedUpByCarrier() {
        Order order = pendingOrder();
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        order.approve("pay-1", now);
        order.confirmPreparation(now);
        order.markShipped("ship-1", now);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertOrderingError(OrderingErrorCode.ORDER_ALREADY_PICKED_UP,
                () -> orderService.cancel(
                        new CancelOrderCommand(ORDER_ID, USER_ID, "too late")));
    }

    @Test
    void cancelReleasesReservationsAndRefundsPaidOrder() {
        Order order = approvedReservedOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(stockReservationGateway).release("res-1", "order-cancelled");
        verify(paymentGateway).refund(eq("pay-1"), eq(BigDecimal.valueOf(100)), eq("VND"),
                eq("user cancellation"), eq("order:order-1:refund:pay-1"));
        verify(compensationTaskPort).markCompleted("refund:order-1:pay-1");
    }

    @Test
    void cancelEnqueuesReservationReleaseWhenImmediateReleaseFails() {
        Order order = orderWithReservation();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("inventory unavailable"))
                .when(stockReservationGateway).release("res-1", "order-cancelled");

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        ArgumentCaptor<CompensationTaskPort.Task> captor =
                ArgumentCaptor.forClass(CompensationTaskPort.Task.class);
        verify(compensationTaskPort).enqueue(captor.capture());
        assertEquals(CompensationTaskPort.Type.RESERVATION_RELEASE,
                captor.getValue().type());
        assertEquals("reservation-release:res-1", captor.getValue().taskId());
    }

    @Test
    void cancelEnqueuesFlashSaleReleaseWhenImmediateReleaseFails() {
        Order order = pendingOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("promotion unavailable"))
                .when(flashSaleQueryPort).release(ORDER_ID);

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        ArgumentCaptor<CompensationTaskPort.Task> captor =
                ArgumentCaptor.forClass(CompensationTaskPort.Task.class);
        verify(compensationTaskPort).enqueue(captor.capture());
        assertEquals(CompensationTaskPort.Type.FLASH_SALE_RELEASE,
                captor.getValue().type());
        assertEquals("flash-sale-release:" + ORDER_ID, captor.getValue().taskId());
    }

    @Test
    void cancelToleratesCompensationPersistenceFailure() {
        Order order = orderWithReservation();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("inventory unavailable"))
                .when(stockReservationGateway).release("res-1", "order-cancelled");
        doThrow(new RuntimeException("task store down"))
                .when(compensationTaskPort).enqueue(any(CompensationTaskPort.Task.class));

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelToleratesRefundFailure() {
        Order order = approvedReservedOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("payment gateway down"))
                .when(paymentGateway).refund(anyString(), any(), anyString(), anyString(),
                        anyString());

        Order result = orderService.cancel(
                new CancelOrderCommand(ORDER_ID, USER_ID, "changed mind"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(compensationTaskPort).enqueue(any(CompensationTaskPort.Task.class));
        verify(compensationTaskPort, never()).markCompleted(anyString());
    }

    @Test
    void cancelOnPaymentFailureSkipsOrderThatIsNoLongerPending() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = orderService.cancelOnPaymentFailure(ORDER_ID, "ERR_001", "late failure");

        assertEquals(OrderStatus.APPROVED, result.getStatus());
        verify(orderPersistencePort, never()).save(any());
        verify(integrationEventPublisher, never()).publishOrderCancelled(anyString(), anyString(),
                anyString(), any());
    }

    @Test
    void cancelOnPaymentFailureThrowsWhenOrderIsMissing() {
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());

        assertOrderingError(OrderingErrorCode.ORDER_NOT_FOUND,
                () -> orderService.cancelOnPaymentFailure(ORDER_ID, "ERR_001", "failure"));
    }

    @Test
    void cancelOnPaymentFailureFallsBackToErrorCodeWhenReasonIsNull() {
        Order order = pendingOrder();
        when(orderPersistencePort.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.cancelOnPaymentFailure(ORDER_ID, "ERR_001", null);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(integrationEventPublisher).publishOrderCancelled(eq(ORDER_ID),
                eq("PAYMENT_FAILED"), eq("ERR_001"),
                eq(OrderingIntegrationEventPublisherPort.CancellationKind.AUTO_CANCELLED));
    }

    @Test
    void rejectByMerchantRejectsOrderOwnedByAnotherMerchant() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        assertOrderingError(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                () -> orderService.rejectByMerchant(
                        new RejectOrderCommand(ORDER_ID, "merchant-2", "no stock")));
    }

    @Test
    void rejectByMerchantReleasesReservationsAndRefundsPaidOrder() {
        Order order = approvedReservedOrder();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.rejectByMerchant(
                new RejectOrderCommand(ORDER_ID, MERCHANT_ID, "no stock"));

        assertEquals(OrderStatus.REJECTED, result.getStatus());
        verify(stockReservationGateway).release("res-1", "merchant-rejected");
        verify(paymentGateway).refund(eq("pay-1"), eq(BigDecimal.valueOf(100)), eq("VND"),
                eq("merchant rejected"), eq("order:order-1:refund:pay-1"));
    }

    // ------------------------------------------------------------------
    // changeShippingInfo guards
    // ------------------------------------------------------------------

    @Test
    void changeShippingInfoRejectsOrderWithInitiatedPayment() {
        Order order = pendingOrder();
        order.approve("pay-1", Instant.parse("2026-07-18T12:00:00Z"));
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        ShippingAddress replacement = new ShippingAddress("addr-2", "User", "+84912345678",
                "99 new street", "WARD-2", "DIST-2", "PROV-2", "VN");

        assertOrderingError(OrderingErrorCode.ORDER_INVALID_STATE,
                () -> orderService.changeShippingInfo(
                        new ChangeShippingInfoCommand(ORDER_ID, USER_ID, replacement)));

        verify(shippingGateway, never()).quote(any(), any(), any(), any(), anyList(), any());
        verify(orderPersistencePort, never()).save(any());
    }

    @Test
    void changeShippingInfoRejectsForeignUser() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        ShippingAddress replacement = new ShippingAddress("addr-2", "User", "+84912345678",
                "99 new street", "WARD-2", "DIST-2", "PROV-2", "VN");

        assertOrderingError(OrderingErrorCode.ORDER_FORBIDDEN,
                () -> orderService.changeShippingInfo(
                        new ChangeShippingInfoCommand(ORDER_ID, "other-user", replacement)));
    }

    // ------------------------------------------------------------------
    // markShipped / reservation commit
    // ------------------------------------------------------------------

    @Test
    void markShippedCommitsReservationsAndCompletesCompensationTasks() {
        Order order = reservedOrderInPreparation();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);

        Order result = orderService.markShipped(
                new ConfirmShippedCommand(ORDER_ID, "ship-1"));

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        verify(stockReservationGateway).commit("res-1");
        verify(compensationTaskPort).markCompleted("stock-commit:res-1");
        verify(compensationTaskPort).enqueue(any(CompensationTaskPort.Task.class));
    }

    @Test
    void markShippedEnqueuesRetryWhenReservationCommitFails() {
        Order order = reservedOrderInPreparation();
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(order)).thenReturn(order);
        doThrow(new RuntimeException("inventory unavailable"))
                .when(stockReservationGateway).commit("res-1");

        Order result = orderService.markShipped(
                new ConfirmShippedCommand(ORDER_ID, "ship-1"));

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        verify(compensationTaskPort).enqueue(any(CompensationTaskPort.Task.class));
        verify(compensationTaskPort, never()).markCompleted("stock-commit:res-1");
    }

    // ------------------------------------------------------------------
    // listByUser / listByMerchantOwner status parsing
    // ------------------------------------------------------------------

    @Test
    void listByUserRejectsUnknownStatusFilter() {
        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.listByUser(USER_ID, "PENDING,BOGUS", 10));
    }

    @Test
    void listByUserTreatsBlankFilterAsUnfiltered() {
        when(orderPersistencePort.findByUser(USER_ID, 10)).thenReturn(List.of(pendingOrder()));

        List<Order> results = orderService.listByUser(USER_ID, " , ", 10);

        assertEquals(1, results.size());
    }

    @Test
    void listByMerchantOwnerPassesTrimmedDistinctStatuses() {
        when(orderPersistencePort.findByMerchantAndStatuses(
                eq(MERCHANT_ID), eq(List.of("PENDING", "SHIPPED")), eq(10)))
                .thenReturn(List.of(pendingOrder()));

        List<Order> results = orderService.listByMerchantOwner(
                MERCHANT_ID, " PENDING , PENDING,SHIPPED ", 10);

        assertEquals(1, results.size());
    }

    @Test
    void getForRequesterRejectsMerchantOwnerOfAnotherMerchant() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-2"))
                .thenReturn(Optional.of("merchant-2"));

        assertOrderingError(OrderingErrorCode.ORDER_FORBIDDEN,
                () -> orderService.getForRequester(ORDER_ID, "owner-2"));
    }

    // ------------------------------------------------------------------
    // analytics: default ranges, invalid ranges, aggregation branches
    // ------------------------------------------------------------------

    @Test
    void getMerchantAnalyticsUsesDefaultRangeAndCurrency() {
        when(orderPersistencePort.findMerchantAnalyticsRows(eq(MERCHANT_ID), any(), any()))
                .thenReturn(List.of());

        var result = orderService.getMerchantAnalytics(MERCHANT_ID, null, null);

        assertEquals(LocalDate.of(2026, 7, 12), result.from());
        assertEquals(LocalDate.of(2026, 7, 18), result.to());
        assertEquals("VND", result.currency());
        assertEquals(7, result.revenueTrend().size());
        assertEquals(0, result.statusBreakdown().size());
        assertEquals(0, result.totalOrders());
    }

    @Test
    void getMerchantAnalyticsRejectsInvertedRange() {
        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.getMerchantAnalytics(MERCHANT_ID,
                        LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 17)));
    }

    @Test
    void getMerchantAnalyticsRejectsRangeLongerThanOneYear() {
        when(orderPersistencePort.findMerchantAnalyticsRows(eq(MERCHANT_ID), any(), any()))
                .thenReturn(List.of());

        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.getMerchantAnalytics(MERCHANT_ID,
                        LocalDate.of(2025, 1, 1), LocalDate.of(2026, 7, 18)));
    }

    @Test
    void getMerchantAnalyticsRejectsMultipleCurrenciesIgnoringBlankValues() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant day = LocalDate.of(2026, 7, 18).atStartOfDay(zone).toInstant();
        when(orderPersistencePort.findMerchantAnalyticsRows(eq(MERCHANT_ID), any(), any()))
                .thenReturn(List.of(
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "COMPLETED", BigDecimal.TEN, "VND", day),
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "PENDING", BigDecimal.ONE, "usd", day),
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "PENDING", BigDecimal.ONE, " ", day)));

        assertThatThrownBy(() -> orderService.getMerchantAnalytics(MERCHANT_ID,
                LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 18)))
                .isInstanceOf(OrderingException.class)
                .hasMessageContaining("multiple currencies");
    }

    @Test
    void getMerchantAnalyticsSkipsNonCompletedOutOfRangeAndNullAmounts() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate from = LocalDate.of(2026, 7, 17);
        LocalDate to = LocalDate.of(2026, 7, 18);
        Instant inRange = to.atStartOfDay(zone).toInstant();
        Instant outOfRange = LocalDate.of(2026, 7, 10).atStartOfDay(zone).toInstant();
        when(orderPersistencePort.findMerchantAnalyticsRows(eq(MERCHANT_ID), any(), any()))
                .thenReturn(List.of(
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "COMPLETED", null, "VND", inRange),
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "PENDING", BigDecimal.valueOf(500), "VND", inRange),
                        new OrderPersistencePort.OrderAnalyticsRow(
                                "COMPLETED", BigDecimal.valueOf(999), "VND", outOfRange)));

        var result = orderService.getMerchantAnalytics(MERCHANT_ID, from, to);

        assertEquals(3, result.totalOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(0, result.totalRevenue().compareTo(BigDecimal.ZERO));
        var point = result.revenueTrend().stream()
                .filter(p -> p.date().equals(to))
                .findFirst()
                .orElseThrow();
        assertEquals(1, point.orders());
        assertEquals(0, point.revenue().compareTo(BigDecimal.ZERO));
        // The out-of-range COMPLETED row still contributes to the status counts.
        assertEquals(2, result.statusBreakdown().stream()
                .filter(s -> "COMPLETED".equals(s.status()))
                .findFirst()
                .orElseThrow()
                .count());
        assertEquals(1, result.statusBreakdown().stream()
                .filter(s -> "PENDING".equals(s.status()))
                .findFirst()
                .orElseThrow()
                .count());
    }

    @Test
    void getPlatformAnalyticsUsesDefaultRangeAndCurrency() {
        when(orderPersistencePort.findPlatformAnalyticsRows(any(), any()))
                .thenReturn(List.of());

        var result = orderService.getPlatformAnalytics(null, null);

        assertEquals(LocalDate.of(2026, 6, 19), result.from());
        assertEquals(LocalDate.of(2026, 7, 18), result.to());
        assertEquals("VND", result.currency());
        assertEquals(30, result.revenueTrend().size());
        assertEquals(0, result.topMerchants().size());
    }

    @Test
    void getPlatformAnalyticsRejectsInvertedRange() {
        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.getPlatformAnalytics(
                        LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 17)));
    }

    @Test
    void getPlatformAnalyticsRejectsMultipleCurrencies() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant day = LocalDate.of(2026, 7, 18).atStartOfDay(zone).toInstant();
        when(orderPersistencePort.findPlatformAnalyticsRows(any(), any()))
                .thenReturn(List.of(
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                MERCHANT_ID, "COMPLETED", BigDecimal.TEN, "VND", day),
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                "merchant-2", "PENDING", BigDecimal.ONE, "USD", day)));

        assertThatThrownBy(() -> orderService.getPlatformAnalytics(
                LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 18)))
                .isInstanceOf(OrderingException.class)
                .hasMessageContaining("multiple currencies");
    }

    @Test
    void getPlatformAnalyticsAggregatesTopMerchantsAndSkipsNonCompletedRows() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate from = LocalDate.of(2026, 7, 17);
        LocalDate to = LocalDate.of(2026, 7, 18);
        Instant day18 = LocalDate.of(2026, 7, 18).atStartOfDay(zone).toInstant();
        Instant day17 = LocalDate.of(2026, 7, 17).atStartOfDay(zone).toInstant();
        Instant outOfRange = LocalDate.of(2026, 7, 10).atStartOfDay(zone).toInstant();
        when(orderPersistencePort.findPlatformAnalyticsRows(any(), any()))
                .thenReturn(List.of(
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                "merchant-2", "COMPLETED", BigDecimal.valueOf(300), "VND", day18),
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                MERCHANT_ID, "COMPLETED", BigDecimal.valueOf(100), "VND", day18),
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                MERCHANT_ID, "COMPLETED", BigDecimal.valueOf(50), "VND", outOfRange),
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                "merchant-3", "PENDING", BigDecimal.valueOf(999), "VND", day18),
                        new OrderPersistencePort.PlatformAnalyticsRow(
                                MERCHANT_ID, "COMPLETED", null, "VND", day17)));

        var result = orderService.getPlatformAnalytics(from, to);

        assertEquals(5, result.totalOrders());
        assertEquals(4, result.completedOrders());
        assertEquals(0, result.totalGmv().compareTo(BigDecimal.valueOf(450)));
        assertEquals(2, result.topMerchants().size());
        assertEquals("merchant-2", result.topMerchants().getFirst().merchantId());
        assertEquals(MERCHANT_ID, result.topMerchants().get(1).merchantId());
        assertEquals(3, result.topMerchants().get(1).orders());
        assertEquals(1, result.statusBreakdown().stream()
                .filter(s -> "PENDING".equals(s.status()))
                .findFirst()
                .orElseThrow()
                .count());
    }

    @Test
    void getMerchantTopProductsClampsLimitToConfiguredBounds() {
        when(orderPersistencePort.findMerchantTopProductRows(eq(MERCHANT_ID), any(), any()))
                .thenReturn(List.of(
                        new OrderPersistencePort.TopProductRow("sku-1", 3, BigDecimal.valueOf(300)),
                        new OrderPersistencePort.TopProductRow("sku-2", 2, BigDecimal.valueOf(200)),
                        new OrderPersistencePort.TopProductRow("sku-3", 1, BigDecimal.valueOf(100))));

        var zeroLimit = orderService.getMerchantTopProducts(MERCHANT_ID, null, null, 0);
        var hugeLimit = orderService.getMerchantTopProducts(MERCHANT_ID, null, null, 10_000);

        assertEquals(1, zeroLimit.size());
        assertEquals(3, hugeLimit.size());
    }

    @Test
    void getMerchantTopProductsRejectsInvertedRange() {
        assertOrderingError(OrderingErrorCode.INVALID_ARGUMENT,
                () -> orderService.getMerchantTopProducts(MERCHANT_ID,
                        LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 17), 5));
    }
}
