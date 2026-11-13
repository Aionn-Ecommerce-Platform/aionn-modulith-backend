package com.aionn.ordering.application.service;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.command.ChangeShippingInfoCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmPreparationCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.TopProductResult;
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
import com.aionn.ordering.application.policy.OrderDefaultsPolicy;
import com.aionn.ordering.application.policy.OrderingAnalyticsPolicy;
import com.aionn.ordering.application.policy.ReservationPolicy;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.promotion.FlashSaleQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final CartPersistencePort cartPersistencePort;
    private final OrderPersistencePort orderPersistencePort;
    private final EventPublisher eventPublisher;
    private final StockReservationGateway stockReservationGateway;
    private final PaymentGateway paymentGateway;
    private final ShippingGateway shippingGateway;
    private final CatalogPricingGateway catalogPricingGateway;
    private final VoucherGateway voucherGateway;
    private final CartService cartService;
    private final MerchantQueryPort merchantQueryPort;
    private final OrderingIntegrationEventPublisherPort integrationEventPublisher;
    private final ReservationPolicy reservationPolicy;
    private final OrderDefaultsPolicy orderDefaultsPolicy;
    private final OrderingAnalyticsPolicy analyticsPolicy;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final CompensationTaskPort compensationTaskPort;
    private final OrderPlacementOperationPort placementOperationPort;
    private final FlashSaleQueryPort flashSaleQueryPort;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order placeOrder(PlaceOrderCommand command) {
        validatePaymentSelection(command.gateway(), command.paymentMethodId());
        Cart cart = cartService.loadOwned(command.userId());
        if (cart.isEmpty()) {
            throw new OrderingException(OrderingErrorCode.CART_EMPTY);
        }

        List<Map.Entry<String, Integer>> cartLines = cart.snapshot();
        List<PlaceOrderHeadlessCommand.Line> lines;
        boolean hasExplicitSelection = command.selectedSkuIds() != null && !command.selectedSkuIds().isEmpty();
        if (hasExplicitSelection) {
            Set<String> selectedSkuIds = new HashSet<>(command.selectedSkuIds());
            lines = cartLines.stream()
                    .filter(e -> selectedSkuIds.contains(e.getKey()))
                    .map(e -> new PlaceOrderHeadlessCommand.Line(e.getKey(), e.getValue()))
                    .toList();
            if (lines.size() != selectedSkuIds.size()) {
                throw new OrderingException(OrderingErrorCode.CART_ITEM_NOT_FOUND,
                        "One or more selected cart items are no longer available in the cart");
            }
        } else {
            lines = cartLines.stream()
                    .map(e -> new PlaceOrderHeadlessCommand.Line(e.getKey(), e.getValue()))
                    .toList();
        }
        if (lines.isEmpty()) {
            throw new OrderingException(OrderingErrorCode.CART_EMPTY);
        }

        Order result = placeFromLines(
                command.userId(),
                lines,
                cart.getVoucherCode(),
                command.paymentMethodId(),
                command.currency(),
                command.shippingAddressSnapshot(), command.idempotencyKey(), command.note(),
                command.shippingService());

        if (result.getStatus() == OrderStatus.APPROVED) {
            transactionTemplate.executeWithoutResult(status -> removePurchasedItemsFromCart(command.userId(), lines));
        }

        return result;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order placeOrderHeadless(PlaceOrderHeadlessCommand command) {
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new OrderingException(OrderingErrorCode.CART_EMPTY,
                    "Headless placement requires at least one line");
        }
        return placeFromLines(
                command.userId(),
                command.lines(),
                command.voucherCode(),
                command.paymentMethodId(),
                command.currency(),
                command.shippingAddressSnapshot(), command.idempotencyKey(), command.note(),
                command.shippingService());
    }

    private Order placeFromLines(
            String userId,
            List<PlaceOrderHeadlessCommand.Line> lines,
            String voucherCode,
            String paymentMethodId,
            String requestCurrency,
            ShippingAddress shippingAddress,
            String requestedIdempotencyKey,
            String note,
            String shippingService) {

        if (lines == null || lines.isEmpty() || lines.size() > 100) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                    "Order must contain between 1 and 100 lines");
        }
        java.util.Set<String> seenSkus = new java.util.HashSet<>();
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            if (line.skuId() == null || line.skuId().isBlank() || line.qty() <= 0) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Order line requires a non-blank SKU and positive quantity");
            }
            if (!seenSkus.add(line.skuId())) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Duplicate SKU in order: " + line.skuId());
            }
        }

        List<String> skuIds = lines.stream().map(PlaceOrderHeadlessCommand.Line::skuId).toList();
        Map<String, CatalogPricingGateway.SkuPricing> pricing = catalogPricingGateway.resolve(skuIds);

        for (PlaceOrderHeadlessCommand.Line line : lines) {
            CatalogPricingGateway.SkuPricing skuInfo = pricing.get(line.skuId());
            if (skuInfo == null || !skuInfo.active() || skuInfo.price() == null) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "SKU " + line.skuId() + " is not available for sale");
            }
        }

        CatalogPricingGateway.SkuPricing first = pricing.values().iterator().next();
        String merchantId = first.merchantId();
        String pricingCurrency = first.currency();
        for (CatalogPricingGateway.SkuPricing p : pricing.values()) {
            if (!merchantId.equals(p.merchantId())) {
                throw new OrderingException(OrderingErrorCode.ORDER_MULTI_MERCHANT_NOT_SUPPORTED);
            }
            if (!pricingCurrency.equals(p.currency())) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Cart contains SKUs with mixed currencies");
            }
        }

        if (requestCurrency != null && !requestCurrency.equals(pricingCurrency)) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Request currency " + requestCurrency
                            + " does not match catalog pricing currency " + pricingCurrency);
        }
        String currency = pricingCurrency;
        String idempotencyKey = (requestedIdempotencyKey == null || requestedIdempotencyKey.isBlank())
                ? "legacy:" + IdGenerator.ulid()
                : requestedIdempotencyKey.trim();
        String requestHash = placementRequestHash(
                userId, lines, voucherCode, paymentMethodId, currency, shippingAddress);
        OrderPlacementOperationPort.Operation operation = placementOperationPort.start(
                userId, idempotencyKey, requestHash, IdGenerator.ulid());
        String orderId = operation.orderId();
        if (!operation.completed() && !operation.claimed()) {
            throw new OrderingException(OrderingErrorCode.ORDER_PLACEMENT_IN_PROGRESS,
                    "An order with this idempotency key is already being placed");
        }
        Order replay = orderPersistencePort.findById(orderId).orElse(null);
        if (replay != null) {
            placementOperationPort.complete(userId, idempotencyKey, orderId);
            return replay;
        }

        List<ShippingGateway.Parcel> parcels = toParcels(lines, pricing);
        Money goodsSubtotal = linesSubtotal(lines, pricing, currency);
        Money shippingFee = quoteShippingFee(orderId, merchantId, shippingAddress, currency, parcels,
                goodsSubtotal.amount(), shippingService);
        Map<String, Integer> flashSaleQuantities = new LinkedHashMap<>();
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            String registrationId = pricing.get(line.skuId()).flashSaleRegistrationId();
            if (registrationId != null) {
                flashSaleQuantities.merge(registrationId, line.qty(), Integer::sum);
            }
        }
        List<FlashSaleQueryPort.Allocation> flashSaleAllocations = flashSaleQuantities.entrySet().stream()
                .map(entry -> new FlashSaleQueryPort.Allocation(entry.getKey(), entry.getValue()))
                .toList();
        try {
            if (!flashSaleAllocations.isEmpty()) {
                flashSaleQueryPort.reserve(orderId, flashSaleAllocations);
            }
        } catch (FlashSaleQueryPort.CapacityExceededException ex) {
            throw new OrderingException(OrderingErrorCode.ORDER_RESERVATION_FAILED,
                    "Flash-sale stock is no longer available");
        }
        List<StockReservationGateway.ReservationLine> reservationLines = lines.stream()
                .map(line -> {
                    CatalogPricingGateway.SkuPricing p = pricing.get(line.skuId());
                    return new StockReservationGateway.ReservationLine(
                            line.skuId(), p.warehouseId(), line.qty(),
                            p.price(), currency);
                }).toList();
        List<StockReservationGateway.Reservation> reservations;
        int ttlSeconds = reservationPolicy.ttlSeconds();
        try {
            reservations = stockReservationGateway.reserveAll(orderId, reservationLines, ttlSeconds);
        } catch (StockReservationGateway.ReservationException ex) {
            releaseFlashSaleBestEffort(orderId);
            throw new OrderingException(OrderingErrorCode.ORDER_RESERVATION_FAILED,
                    "Reservation failed for SKU " + ex.getSkuId() + ": " + ex.getMessage());
        } catch (RuntimeException ex) {
            releaseFlashSaleBestEffort(orderId);
            throw ex;
        }

        List<OrderItem> items = new ArrayList<>(reservations.size());
        Money lineSubtotal = Money.zero(currency);
        for (StockReservationGateway.Reservation r : reservations) {
            Money unit = Money.of(r.unitPrice(), currency);
            CatalogPricingGateway.SkuPricing sku = pricing.get(r.skuId());
            items.add(new OrderItem(r.skuId(), r.qty(), unit, r.warehouseId(), r.reservationId(),
                    sku == null ? null : sku.productName(),
                    sku == null ? null : sku.imageUrl(),
                    sku == null ? null : sku.variantLabel()));
            lineSubtotal = lineSubtotal.add(unit.multiply(r.qty()));
        }

        Money subtotal = lineSubtotal;
        Order placed;
        try {
            // Voucher consumption and order persistence commit atomically; a failed save rolls it back.
            placed = transactionTemplate.execute(status -> {
                Money discountedSubtotal = subtotal;
                Money discountedShipping = shippingFee;
                if (voucherCode != null) {
                    List<String> orderCategoryIds = pricing.values().stream()
                            .flatMap(sku -> sku.categoryIds().stream())
                            .distinct()
                            .toList();
                    VoucherGateway.Discount discount = voucherGateway.apply(
                            userId, merchantId, voucherCode, orderId, subtotal.amount(), currency,
                            orderCategoryIds);
                    if (!discount.valid()) {
                        throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                                "Voucher invalid: " + discount.reason());
                    }
                    validateVoucherDiscount(discount, currency);
                    if (discount.freeShipping()) {
                        // A free-shipping voucher waives the fee, capped by its own amount.
                        BigDecimal waived = discount.amount().min(shippingFee.amount());
                        discountedShipping = Money.of(
                                shippingFee.amount().subtract(waived).max(BigDecimal.ZERO), currency);
                    } else {
                        BigDecimal newSubtotal = subtotal.amount()
                                .subtract(discount.amount()).max(BigDecimal.ZERO);
                        discountedSubtotal = Money.of(newSubtotal, currency);
                    }
                }

                String proposalId = "prop-" + IdGenerator.ulid();
                Order order = Order.place(orderId, userId, merchantId, proposalId,
                        paymentMethodId, currency, items, shippingAddress, discountedShipping, discountedSubtotal,
                        clock.instant());
                order.attachNote(note);
                order.attachShippingService(shippingService);
                Order saved = orderPersistencePort.save(order);
                eventPublisher.publish(order.pullEvents());
                integrationEventPublisher.publishOrderPlaced(saved);
                if (paymentMethodId == null || "COD".equalsIgnoreCase(paymentMethodId)) {
                    saved.approve(null, clock.instant());
                    saved = orderPersistencePort.save(saved);
                    eventPublisher.publish(saved.pullEvents());
                    integrationEventPublisher.publishOrderApproved(saved.getOrderId(), null);
                }
                return saved;
            });
        } catch (RuntimeException failure) {
            compensateFailedPlacement(orderId, reservations);
            throw failure;
        }
        // Order committed: a failure to record completion must not release its stock/voucher.
        placementOperationPort.complete(userId, idempotencyKey, orderId);
        return placed;
    }

    public Order approvePayment(String orderId, String paymentId) {
        return approvePayment(orderId, paymentId, null, null);
    }

    public Order approvePayment(String orderId, String paymentId, BigDecimal capturedAmount, String capturedCurrency) {
        // Pessimistic row lock so a concurrent cancelOnPaymentFailure can't race this approve.
        Order saved = transactionTemplate.execute(status -> {
            Order order = orderPersistencePort.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
            if (order.getStatus() == OrderStatus.APPROVED) {
                if (java.util.Objects.equals(order.getPaymentId(), paymentId)) {
                    return order;
                }
                refundIfPaid(order, paymentId, capturedAmount, capturedCurrency, "duplicate payment capture");
                return order;
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                refundIfPaid(order, paymentId, capturedAmount, capturedCurrency,
                        "payment captured after cancellation");
                return order;
            }
            order.approve(paymentId, clock.instant());
            Order persisted = orderPersistencePort.save(order);
            eventPublisher.publish(persisted.pullEvents());
            integrationEventPublisher.publishOrderApproved(persisted.getOrderId(), paymentId);
            return persisted;
        });
        List<PlaceOrderHeadlessCommand.Line> lines = saved.getItems().stream()
                .map(item -> new PlaceOrderHeadlessCommand.Line(item.skuId(), item.qty()))
                .toList();
        removePurchasedItemsFromCart(saved.getUserId(), lines);
        return saved;
    }

    private void removePurchasedItemsFromCart(String userId, List<PlaceOrderHeadlessCommand.Line> lines) {
        Cart freshCart = cartService.loadOwned(userId);
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            freshCart.removeItemIfPresent(line.skuId(), clock.instant());
        }
        if (freshCart.isEmpty()) {
            freshCart.clear("order-placed", clock.instant());
        } else if (freshCart.getVoucherCode() != null) {
            freshCart.removeVoucher(clock.instant());
        }
        cartPersistencePort.save(freshCart);
        eventPublisher.publish(freshCart.pullEvents());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order confirmPreparation(ConfirmPreparationCommand command) {
        Order saved = transactionTemplate.execute(status -> {
            String merchantId = requireMerchantIdForOwner(command.ownerId());
            Order order = ownedByMerchant(command.orderId(), merchantId);
            order.confirmPreparation(clock.instant());
            Order persisted = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            return persisted;
        });
        triggerShipmentRegistration(saved);
        return saved;
    }

    private void triggerShipmentRegistration(Order order) {
        try {
            BigDecimal fee = order.getShippingFee() == null ? null : order.getShippingFee().amount();
            shippingGateway.createAndRegister(order.getOrderId(), order.getMerchantId(),
                    order.getUserId(), order.getShippingAddress(), null, fee, order.getCurrency(),
                    toParcels(order));
        } catch (Exception ex) {
            log.warn("Auto-shipment registration failed for order {}: {}",
                    order.getOrderId(), ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order cancel(CancelOrderCommand command) {
        Order saved = transactionTemplate.execute(status -> {
            Order order = ownedByUser(command.orderId(), command.userId());
            if (order.getStatus().isPickedUpByCarrier()) {
                throw new OrderingException(OrderingErrorCode.ORDER_ALREADY_PICKED_UP);
            }
            order.cancel("USER_CANCELLED", command.reason(), clock.instant());
            Order persisted = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            integrationEventPublisher.publishOrderCancelled(persisted.getOrderId(), "USER_CANCELLED",
                    command.reason(), OrderingIntegrationEventPublisherPort.CancellationKind.USER_CANCELLED);
            return persisted;
        });
        releaseReservationsBestEffort(saved, "order-cancelled");
        refundIfPaid(saved, saved.getPaymentId(), null, null, "user cancellation");
        return saved;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order cancelOnPaymentFailure(String orderId, String errorCode, String reason) {
        Order saved = transactionTemplate.execute(status -> {
            Order order = orderPersistencePort.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
            if (order.getStatus() != OrderStatus.PENDING) {
                log.info("Skipping payment-failure cancel for order {}: status is {}",
                        orderId, order.getStatus());
                return order;
            }
            order.autoCancel("PAYMENT_FAILED", clock.instant());
            Order persisted = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            integrationEventPublisher.publishOrderCancelled(persisted.getOrderId(), "PAYMENT_FAILED",
                    reason == null ? errorCode : reason,
                    OrderingIntegrationEventPublisherPort.CancellationKind.AUTO_CANCELLED);
            return persisted;
        });
        if (saved.getStatus() == OrderStatus.CANCELLED) {
            releaseReservationsBestEffort(saved, "payment-failed");
        }
        return saved;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order rejectByMerchant(RejectOrderCommand command) {
        Order saved = transactionTemplate.execute(status -> {
            String merchantId = requireMerchantIdForOwner(command.ownerId());
            Order order = ownedByMerchant(command.orderId(), merchantId);
            order.rejectByMerchant(merchantId, command.reason(), clock.instant());
            Order persisted = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            integrationEventPublisher.publishOrderCancelled(persisted.getOrderId(), "MERCHANT_REJECTED",
                    command.reason(), OrderingIntegrationEventPublisherPort.CancellationKind.MERCHANT_REJECTED);
            return persisted;
        });
        releaseReservationsBestEffort(saved, "merchant-rejected");
        refundIfPaid(saved, saved.getPaymentId(), null, null, "merchant rejected");
        return saved;
    }

    private void releaseReservationsBestEffort(Order order, String reason) {
        releaseFlashSaleBestEffort(order.getOrderId());
        for (OrderItem item : order.items()) {
            if (item.reservationId() == null) {
                continue;
            }
            try {
                stockReservationGateway.release(item.reservationId(), reason);
            } catch (RuntimeException ex) {
                log.warn("Release of reservation {} ({}) failed: {}",
                        item.reservationId(), reason, ex.getMessage());
                enqueueReservationRelease(item.reservationId(), reason);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order changeShippingInfo(ChangeShippingInfoCommand command) {
        Order snapshot = transactionTemplate.execute(status -> ownedByUser(command.orderId(), command.userId()));
        // A captured payment is locked to the old total; re-pricing would diverge from the gateway.
        if (snapshot.getPaymentId() != null) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Cannot change shipping after payment has been initiated");
        }
        Money newFee = quoteShippingFee(snapshot.getOrderId(), snapshot.getMerchantId(),
                command.newAddress(), snapshot.getCurrency(), toParcels(snapshot),
                orderGoodsSubtotal(snapshot));
        return transactionTemplate.execute(status -> {
            Order order = ownedByUser(command.orderId(), command.userId());
            order.changeShippingInfo(command.newAddress(), newFee, clock.instant());
            Order saved = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            return saved;
        });
    }

    private static String placementRequestHash(String userId,
            List<PlaceOrderHeadlessCommand.Line> lines, String voucherCode, String paymentMethodId,
            String currency, ShippingAddress address) {
        String canonicalLines = lines.stream()
                .sorted(Comparator.comparing(PlaceOrderHeadlessCommand.Line::skuId))
                .map(line -> line.skuId() + ":" + line.qty())
                .collect(Collectors.joining(","));
        return Sha256Hasher.hexDigest(String.join("|",
                userId, canonicalLines, String.valueOf(voucherCode), String.valueOf(paymentMethodId),
                currency, String.valueOf(address)));
    }

    private static void validateVoucherDiscount(VoucherGateway.Discount discount, String orderCurrency) {
        if (discount.amount() == null || discount.amount().signum() < 0) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Voucher returned an invalid discount amount");
        }
        if (discount.currency() == null || !discount.currency().equals(orderCurrency)) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Voucher currency does not match order currency");
        }
    }

    /** A SKU without a weight rejects the quote; fees must come from real parcel data. */
    private List<ShippingGateway.Parcel> toParcels(
            List<PlaceOrderHeadlessCommand.Line> lines,
            Map<String, CatalogPricingGateway.SkuPricing> pricing) {
        List<ShippingGateway.Parcel> parcels = new ArrayList<>(lines.size());
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            CatalogPricingGateway.SkuPricing sku = pricing.get(line.skuId());
            CatalogPricingGateway.SkuPricing.Parcel p = sku == null ? null : sku.parcel();
            if (p == null || !p.hasWeight()) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "SKU " + line.skuId() + " is missing its shipping weight");
            }
            int totalWeight = p.weightGram() * line.qty();
            parcels.add(new ShippingGateway.Parcel(
                    totalWeight, p.lengthCm(), p.widthCm(), p.heightCm(), line.qty()));
        }
        return parcels;
    }

    /** Item dims are not stored on the order, so re-resolve pricing to recover parcels. */
    private List<ShippingGateway.Parcel> toParcels(Order order) {
        List<String> skuIds = order.getItems().stream().map(OrderItem::skuId).toList();
        Map<String, CatalogPricingGateway.SkuPricing> pricing = catalogPricingGateway.resolve(skuIds);
        List<PlaceOrderHeadlessCommand.Line> lines = order.getItems().stream()
                .map(item -> new PlaceOrderHeadlessCommand.Line(item.skuId(), item.qty()))
                .toList();
        return toParcels(lines, pricing);
    }

    private Money linesSubtotal(List<PlaceOrderHeadlessCommand.Line> lines,
            Map<String, CatalogPricingGateway.SkuPricing> pricing, String currency) {
        Money subtotal = Money.zero(currency);
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            CatalogPricingGateway.SkuPricing sku = pricing.get(line.skuId());
            if (sku == null || sku.price() == null) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "SKU " + line.skuId() + " has no price for the subtotal");
            }
            subtotal = subtotal.add(Money.of(sku.price(), currency).multiply(line.qty()));
        }
        return subtotal;
    }

    private BigDecimal orderGoodsSubtotal(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.items()) {
            subtotal = subtotal.add(item.unitPrice().amount()
                    .multiply(BigDecimal.valueOf(item.qty())));
        }
        return subtotal;
    }

    private Money quoteShippingFee(String orderId, String merchantId,
            ShippingAddress address, String currency,
            List<ShippingGateway.Parcel> parcels, BigDecimal goodsSubtotal) {
        return quoteShippingFee(orderId, merchantId, address, currency, parcels, goodsSubtotal, null);
    }

    private Money quoteShippingFee(String orderId, String merchantId,
            ShippingAddress address, String currency,
            List<ShippingGateway.Parcel> parcels, BigDecimal goodsSubtotal, String shippingService) {
        ShippingGateway.ShippingQuote quote = shippingService == null || shippingService.isBlank()
                ? shippingGateway.quote(orderId, merchantId, address, currency, parcels, goodsSubtotal)
                : shippingGateway.quote(orderId, merchantId, address, currency, parcels, goodsSubtotal,
                        shippingService);
        if (quote == null || quote.fee() == null || quote.currency() == null
                || quote.fee().signum() < 0 || !currency.equalsIgnoreCase(quote.currency())) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Shipping quote is missing, negative, or uses a different currency");
        }
        return Money.of(quote.fee(), currency);
    }

    private void refundIfPaid(Order order, String paymentId, BigDecimal capturedAmount,
            String capturedCurrency, String reason) {
        if (paymentId == null) {
            return;
        }
        BigDecimal amount = capturedAmount != null ? capturedAmount : order.getTotalAmount().amount();
        String currency = capturedCurrency != null ? capturedCurrency : order.getCurrency();
        String taskId = "refund:" + order.getOrderId() + ":" + paymentId;
        String idempotencyKey = "order:" + order.getOrderId() + ":refund:" + paymentId;
        enqueueCompensation(new CompensationTaskPort.Task(
                taskId, CompensationTaskPort.Type.REFUND, paymentId,
                order.getUserId(), order.getOrderId(), reason, 0));
        try {
            paymentGateway.refund(paymentId, amount, currency, reason, idempotencyKey);
            completeCompensation(taskId);
        } catch (RuntimeException ex) {
            log.warn("Refund for order {} payment {} failed; retry task {} remains pending",
                    order.getOrderId(), paymentId, taskId, ex);
        }
    }

    private void completeCompensation(String taskId) {
        try {
            compensationTaskPort.markCompleted(taskId);
        } catch (RuntimeException ex) {
            log.warn("Unable to mark compensation task {} completed", taskId, ex);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order markShipped(ConfirmShippedCommand command) {
        Order snapshot = transactionTemplate.execute(status -> {
            Order order = required(command.orderId());
            order.markShipped(command.shipmentId(), clock.instant());
            Order saved = orderPersistencePort.save(order);
            eventPublisher.publish(order.pullEvents());
            integrationEventPublisher.publishOrderShipped(saved.getOrderId(), command.shipmentId());
            return saved;
        });
        commitReservationsBestEffort(snapshot);
        return snapshot;
    }

    private void commitReservationsBestEffort(Order order) {
        for (OrderItem item : order.items()) {
            if (item.reservationId() == null) {
                continue;
            }
            String taskId = "stock-commit:" + item.reservationId();
            enqueueCompensation(new CompensationTaskPort.Task(
                    taskId, CompensationTaskPort.Type.STOCK_COMMIT, item.reservationId(),
                    null, order.getOrderId(), "order-shipped", 0));
            try {
                stockReservationGateway.commit(item.reservationId());
                completeCompensation(taskId);
            } catch (RuntimeException ex) {
                log.warn("Stock commit for reservation {} failed; retry task {} remains pending",
                        item.reservationId(), taskId, ex);
            }
        }
    }

    public Order complete(ConfirmDeliveredCommand command) {
        Order order = required(command.orderId());
        order.complete(clock.instant());
        Order saved = orderPersistencePort.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCompleted(saved.getOrderId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getForRequester(String orderId, String requesterUserId) {
        Order order = required(orderId);
        if (order.getUserId().equals(requesterUserId)) {
            return order;
        }
        String requesterMerchantId = merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        if (requesterMerchantId != null && requesterMerchantId.equals(order.getMerchantId())) {
            return order;
        }
        throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<Order> listByUser(String userId, int limit) {
        return orderPersistencePort.findByUser(userId, limit);
    }

    @Transactional(readOnly = true)
    public List<Order> listByUser(String userId, String statusFilter, int limit) {
        List<String> statuses = parseStatusFilter(statusFilter);
        if (statuses.isEmpty()) {
            return listByUser(userId, limit);
        }
        return orderPersistencePort.findByUserAndStatuses(userId, statuses, limit);
    }

    @Transactional(readOnly = true)
    public List<Order> listByMerchantOwner(String ownerId, String statusFilter, int limit) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        List<String> statuses = parseStatusFilter(statusFilter);
        if (statuses.isEmpty()) {
            return orderPersistencePort.findByMerchant(merchantId, limit);
        }
        return orderPersistencePort.findByMerchantAndStatuses(merchantId, statuses, limit);
    }

    @Transactional(readOnly = true)
    public MerchantOrderAnalyticsResult getMerchantAnalytics(String ownerId, LocalDate from, LocalDate to) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        ZoneId zone = analyticsPolicy.zone();
        LocalDate safeTo = to == null ? clock.instant().atZone(zone).toLocalDate() : to;
        LocalDate safeFrom = from == null
                ? safeTo.minusDays(analyticsPolicy.merchantDefaultRangeDays() - 1L)
                : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.OrderAnalyticsRow> rows = orderPersistencePort.findMerchantAnalyticsRows(merchantId, start,
                endExclusive);
        validateAnalyticsRange(safeFrom, safeTo);
        String currency = singleCurrency(rows.stream()
                .map(OrderPersistencePort.OrderAnalyticsRow::currency).toList());

        Map<LocalDate, DailyRevenueAccumulator> revenueByDate = new LinkedHashMap<>();
        for (LocalDate day = safeFrom; !day.isAfter(safeTo); day = day.plusDays(1)) {
            revenueByDate.put(day, new DailyRevenueAccumulator());
        }

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }

        long completedOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (OrderPersistencePort.OrderAnalyticsRow row : rows) {
            String status = row.status();
            statusCounts.merge(status, 1L, Long::sum);
            if (!OrderStatus.COMPLETED.name().equals(status)) {
                continue;
            }

            LocalDate orderDate = row.createdAt().atZone(zone).toLocalDate();
            DailyRevenueAccumulator accumulator = revenueByDate.get(orderDate);
            if (accumulator == null) {
                continue;
            }
            BigDecimal amount = row.totalAmount() == null ? BigDecimal.ZERO : row.totalAmount();
            accumulator.add(amount);
            totalRevenue = totalRevenue.add(amount);
            completedOrders++;
        }

        List<MerchantOrderAnalyticsResult.RevenuePoint> revenueTrend = revenueByDate.entrySet().stream()
                .map(entry -> new MerchantOrderAnalyticsResult.RevenuePoint(
                        entry.getKey(),
                        entry.getValue().revenue(),
                        entry.getValue().orders()))
                .toList();
        List<MerchantOrderAnalyticsResult.StatusCount> statusBreakdown = statusCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new MerchantOrderAnalyticsResult.StatusCount(entry.getKey(), entry.getValue()))
                .toList();

        return new MerchantOrderAnalyticsResult(
                safeFrom,
                safeTo,
                currency,
                totalRevenue,
                rows.size(),
                completedOrders,
                revenueTrend,
                statusBreakdown);
    }

    @Transactional(readOnly = true)
    public PlatformOrderAnalyticsResult getPlatformAnalytics(LocalDate from, LocalDate to) {
        ZoneId zone = analyticsPolicy.zone();
        LocalDate safeTo = to == null ? clock.instant().atZone(zone).toLocalDate() : to;
        LocalDate safeFrom = from == null
                ? safeTo.minusDays(analyticsPolicy.platformDefaultRangeDays() - 1L)
                : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.PlatformAnalyticsRow> rows = orderPersistencePort.findPlatformAnalyticsRows(start,
                endExclusive);
        validateAnalyticsRange(safeFrom, safeTo);
        String currency = singleCurrency(rows.stream()
                .map(OrderPersistencePort.PlatformAnalyticsRow::currency).toList());

        Map<LocalDate, DailyRevenueAccumulator> revenueByDate = new LinkedHashMap<>();
        for (LocalDate day = safeFrom; !day.isAfter(safeTo); day = day.plusDays(1)) {
            revenueByDate.put(day, new DailyRevenueAccumulator());
        }

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }

        Map<String, MerchantAccumulator> byMerchant = new LinkedHashMap<>();
        long completedOrders = 0;
        BigDecimal totalGmv = BigDecimal.ZERO;

        for (OrderPersistencePort.PlatformAnalyticsRow row : rows) {
            statusCounts.merge(row.status(), 1L, Long::sum);
            if (!OrderStatus.COMPLETED.name().equals(row.status())) {
                continue;
            }
            BigDecimal amount = row.totalAmount() == null ? BigDecimal.ZERO : row.totalAmount();
            LocalDate orderDate = row.createdAt().atZone(zone).toLocalDate();
            DailyRevenueAccumulator accumulator = revenueByDate.get(orderDate);
            if (accumulator != null) {
                accumulator.add(amount);
            }
            totalGmv = totalGmv.add(amount);
            completedOrders++;
            byMerchant.computeIfAbsent(row.merchantId(), id -> new MerchantAccumulator()).add(amount);
        }

        List<MerchantOrderAnalyticsResult.RevenuePoint> revenueTrend = revenueByDate.entrySet().stream()
                .map(entry -> new MerchantOrderAnalyticsResult.RevenuePoint(
                        entry.getKey(),
                        entry.getValue().revenue(),
                        entry.getValue().orders()))
                .toList();
        List<MerchantOrderAnalyticsResult.StatusCount> statusBreakdown = statusCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new MerchantOrderAnalyticsResult.StatusCount(entry.getKey(), entry.getValue()))
                .toList();
        List<PlatformOrderAnalyticsResult.TopMerchant> topMerchants = byMerchant.entrySet().stream()
                .map(entry -> new PlatformOrderAnalyticsResult.TopMerchant(
                        entry.getKey(),
                        entry.getValue().revenue(),
                        entry.getValue().orders()))
                .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
                .limit(analyticsPolicy.topMerchantsLimit())
                .toList();

        return new PlatformOrderAnalyticsResult(
                safeFrom,
                safeTo,
                currency,
                totalGmv,
                rows.size(),
                completedOrders,
                revenueTrend,
                statusBreakdown,
                topMerchants);
    }

    @Transactional(readOnly = true)
    public List<TopProductResult> getMerchantTopProducts(String ownerId, LocalDate from, LocalDate to, int limit) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        ZoneId zone = analyticsPolicy.zone();
        LocalDate safeTo = to == null ? clock.instant().atZone(zone).toLocalDate() : to;
        LocalDate safeFrom = from == null
                ? safeTo.minusDays(analyticsPolicy.merchantDefaultRangeDays() - 1L)
                : from;
        validateAnalyticsRange(safeFrom, safeTo);
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }
        int safeLimit = Math.min(Math.max(limit, 1), analyticsPolicy.maxTopProductsLimit());

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.TopProductRow> rows = orderPersistencePort.findMerchantTopProductRows(merchantId, start,
                endExclusive);

        return rows.stream()
                .limit(safeLimit)
                .map(row -> new TopProductResult(row.skuId(), row.unitsSold(), row.revenue()))
                .toList();
    }

    private static final class MerchantAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders = 0;

        void add(BigDecimal amount) {
            revenue = revenue.add(amount);
            orders++;
        }

        BigDecimal revenue() {
            return revenue;
        }

        long orders() {
            return orders;
        }
    }

    private List<String> parseStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return List.of();
        }
        return Arrays.stream(statusFilter.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::parseOrderStatus)
                .distinct()
                .toList();
    }

    private String parseOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status).name();
        } catch (IllegalArgumentException ex) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }
    }

    Order required(String orderId) {
        return orderPersistencePort.findById(orderId)
                .or(() -> orderPersistencePort.findByIdOrOrderSn(orderId))
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
    }

    Order ownedByUser(String orderId, String userId) {
        Order order = required(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
        }
        return order;
    }

    Order ownedByMerchant(String orderId, String merchantId) {
        Order order = required(orderId);
        if (!order.getMerchantId().equals(merchantId)) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT);
        }
        return order;
    }

    /**
     * The {@code @CurrentMerchantId} resolver already verified ownership, so this value is a
     * merchantId — not an owner id to look up again.
     */
    private String requireMerchantIdForOwner(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                    "No merchant registered for the authenticated user");
        }
        return merchantId;
    }

    private void releaseReservations(List<StockReservationGateway.Reservation> reservations, String reason) {
        for (StockReservationGateway.Reservation r : reservations) {
            try {
                stockReservationGateway.release(r.reservationId(), reason);
            } catch (Exception ex) {
                log.warn("Failed to release reservation {} during compensation: {}", r.reservationId(),
                        ex.getMessage());
                enqueueReservationRelease(r.reservationId(), reason);
            }
        }
    }

    private void validatePaymentSelection(String gateway, String paymentMethodId) {
        boolean cod = gateway == null || "COD".equalsIgnoreCase(gateway);
        boolean hasPaymentMethod = paymentMethodId != null && !paymentMethodId.isBlank();
        if (!cod && !hasPaymentMethod) {
            throw new OrderingException(OrderingErrorCode.ORDER_PAYMENT_FAILED,
                    "A payment method is required for gateway " + gateway);
        }
        if (cod && hasPaymentMethod && !"COD".equalsIgnoreCase(paymentMethodId)) {
            throw new OrderingException(OrderingErrorCode.ORDER_PAYMENT_FAILED,
                    "COD gateway requires a COD payment method");
        }
    }

    private String singleCurrency(java.util.List<String> currencies) {
        var distinct = currencies.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
        if (distinct.size() > 1) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                    "Analytics range contains multiple currencies: " + distinct);
        }
        return distinct.isEmpty() ? orderDefaultsPolicy.defaultCurrency() : distinct.getFirst();
    }

    private void validateAnalyticsRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)
                || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 366) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                    "Analytics range must be between 1 and 367 days");
        }
    }

    private void compensateFailedPlacement(String orderId,
            List<StockReservationGateway.Reservation> reservations) {
        releaseFlashSaleBestEffort(orderId);
        // Voucher apply ran inside the rolled-back transaction, so its slot is already restored.
        releaseReservations(reservations, "order-placement-failed");
    }

    private void releaseFlashSaleBestEffort(String orderId) {
        try {
            flashSaleQueryPort.release(orderId);
        } catch (RuntimeException ex) {
            log.error("Failed to release flash-sale allocation for order {}", orderId, ex);
            enqueueCompensation(new CompensationTaskPort.Task(
                    "flash-sale-release:" + orderId,
                    CompensationTaskPort.Type.FLASH_SALE_RELEASE,
                    orderId, null, orderId, "order-placement-failed", 0));
        }
    }

    private void enqueueReservationRelease(String reservationId, String reason) {
        enqueueCompensation(new CompensationTaskPort.Task(
                "reservation-release:" + reservationId,
                CompensationTaskPort.Type.RESERVATION_RELEASE,
                reservationId, null, null, reason, 0));
    }

    private void enqueueCompensation(CompensationTaskPort.Task task) {
        try {
            compensationTaskPort.enqueue(task);
        } catch (RuntimeException persistenceFailure) {
            log.error("Unable to persist compensation task {}", task.taskId(), persistenceFailure);
        }
    }

    public OrderStatus statusOf(String orderId) {
        return required(orderId).getStatus();
    }

    private static final class DailyRevenueAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders;

        void add(BigDecimal amount) {
            revenue = revenue.add(amount);
            orders++;
        }

        BigDecimal revenue() {
            return revenue;
        }

        long orders() {
            return orders;
        }
    }
}
