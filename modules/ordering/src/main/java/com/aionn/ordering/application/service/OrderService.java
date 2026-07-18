package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.command.ChangeShippingInfoCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmPreparationCommand;
import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.TopProductResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.infrastructure.config.OrderingProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CartPersistencePort cartRepository;
    private final OrderPersistencePort orderRepository;
    private final OrderingResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final StockReservationGateway stockReservationGateway;
    private final PaymentGateway paymentGateway;
    private final ShippingGateway shippingGateway;
    private final CatalogPricingGateway catalogPricingGateway;
    private final VoucherGateway voucherGateway;
    private final CartService cartService;
    private final MerchantQueryPort merchantQueryPort;
    private final OrderingIntegrationEventPublisherPort integrationEventPublisher;
    private final OrderingProperties properties;

    public OrderResult placeOrder(PlaceOrderCommand command) {
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

        OrderResult result = placeFromLines(
                command.userId(),
                lines,
                cart.getVoucherCode(),
                command.paymentMethodId(),
                command.currency(),
                command.shippingFee(),
                command.shippingAddressSnapshot());

        // Online payments stay pending until the gateway confirms them; keep
        // cart lines in that case so a failed payment can be retried.
        if (OrderStatus.APPROVED.name().equals(result.status())) {
            removePurchasedItemsFromCart(command.userId(), lines);
        }

        return result;
    }

    /**
     * Headless placement: no cart involvement. Used by UCP agentic checkout
     * where the agent commits a snapshot of line items directly.
     */
    public OrderResult placeOrderHeadless(PlaceOrderHeadlessCommand command) {
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
                command.shippingFee(),
                command.shippingAddressSnapshot());
    }

    /**
     * Core placement flow shared by cart-driven and headless paths:
     * pricing → reservation → voucher → payment → order create.
     */
    private OrderResult placeFromLines(
            String userId,
            List<PlaceOrderHeadlessCommand.Line> lines,
            String voucherCode,
            String paymentMethodId,
            String requestCurrency,
            BigDecimal shippingFeeAmount,
            com.aionn.ordering.domain.valueobject.ShippingAddress shippingAddress) {

        List<String> skuIds = lines.stream().map(PlaceOrderHeadlessCommand.Line::skuId).toList();
        Map<String, CatalogPricingGateway.SkuPricing> pricing = catalogPricingGateway.resolve(skuIds);

        for (PlaceOrderHeadlessCommand.Line line : lines) {
            CatalogPricingGateway.SkuPricing skuInfo = pricing.get(line.skuId());
            if (skuInfo == null || !skuInfo.active()) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "SKU " + line.skuId() + " is not available for sale");
            }
        }

        CatalogPricingGateway.SkuPricing first = pricing.values().iterator().next();
        String merchantId = first.merchantId();
        String pricingCurrency = first.currency();
        for (CatalogPricingGateway.SkuPricing p : pricing.values()) {
            if (!merchantId.equals(p.merchantId())) {
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Cart contains SKUs from multiple merchants - split flow not implemented");
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

        List<StockReservationGateway.ReservationLine> reservationLines = lines.stream()
                .map(line -> {
                    CatalogPricingGateway.SkuPricing p = pricing.get(line.skuId());
                    return new StockReservationGateway.ReservationLine(
                            line.skuId(), p.warehouseId(), line.qty(),
                            p.price(), currency);
                }).toList();
        List<StockReservationGateway.Reservation> reservations;
        int ttlSeconds = properties.reservation().ttlSeconds();
        try {
            reservations = stockReservationGateway.reserveAll(IdGenerator.ulid(), reservationLines, ttlSeconds);
        } catch (StockReservationGateway.ReservationException ex) {
            throw new OrderingException(OrderingErrorCode.ORDER_RESERVATION_FAILED,
                    "Reservation failed for SKU " + ex.getSkuId() + ": " + ex.getMessage());
        }

        List<OrderItem> items = new ArrayList<>(reservations.size());
        Money lineSubtotal = Money.zero(currency);
        for (StockReservationGateway.Reservation r : reservations) {
            Money unit = Money.of(r.unitPrice(), currency);
            items.add(new OrderItem(r.skuId(), r.qty(), unit, r.warehouseId(), r.reservationId()));
            lineSubtotal = lineSubtotal.add(unit.multiply(r.qty()));
        }

        String orderId = IdGenerator.ulid();

        if (voucherCode != null) {
            VoucherGateway.Discount discount = voucherGateway.apply(
                    userId, merchantId, voucherCode, orderId, lineSubtotal.amount(), currency);
            if (!discount.valid()) {
                releaseReservations(reservations, "voucher-invalid");
                throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Voucher invalid: " + discount.reason());
            }
            BigDecimal newSubtotal = lineSubtotal.amount().subtract(discount.amount()).max(BigDecimal.ZERO);
            lineSubtotal = Money.of(newSubtotal, currency);
        }

        Money shippingFee = shippingFeeAmount == null
                ? Money.zero(currency)
                : Money.of(shippingFeeAmount, currency);
        String proposalId = "prop-" + System.nanoTime();
        Order order = Order.place(orderId, userId, merchantId, proposalId,
                paymentMethodId, currency, items, shippingAddress, shippingFee, lineSubtotal);
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderPlaced(saved);
        if (paymentMethodId == null || "COD".equalsIgnoreCase(paymentMethodId)) {
            return approvePayment(saved.getOrderId(), null);
        }
        return mapper.toResult(saved);
    }

    public OrderResult approvePayment(String orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.APPROVED) {
            return mapper.toResult(order);
        }
        order.approve(paymentId);
        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved.pullEvents());
        integrationEventPublisher.publishOrderApproved(saved.getOrderId(), paymentId);

        List<PlaceOrderHeadlessCommand.Line> lines = saved.getItems().stream()
                .map(item -> new PlaceOrderHeadlessCommand.Line(item.skuId(), item.qty()))
                .toList();
        removePurchasedItemsFromCart(saved.getUserId(), lines);
        return mapper.toResult(saved);
    }

    private void removePurchasedItemsFromCart(String userId, List<PlaceOrderHeadlessCommand.Line> lines) {
        Cart freshCart = cartService.loadOwned(userId);
        for (PlaceOrderHeadlessCommand.Line line : lines) {
            freshCart.removeItemIfPresent(line.skuId());
        }
        if (freshCart.isEmpty()) {
            freshCart.clear("order-placed");
        } else if (freshCart.getVoucherCode() != null) {
            freshCart.removeVoucher();
        }
        cartRepository.save(freshCart);
        eventPublisher.publish(freshCart.pullEvents());
    }

    public OrderResult confirmPreparation(ConfirmPreparationCommand command) {
        String merchantId = requireMerchantIdForOwner(command.ownerId());
        Order order = ownedByMerchant(command.orderId(), merchantId);
        order.confirmPreparation();
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        triggerShipmentRegistration(saved);
        return mapper.toResult(saved);
    }

    /**
     * Best-effort: register the shipment with the carrier once the merchant
     * confirms preparation. Failure (e.g. carrier outage) is logged but must
     * not block the order flow - the polling worker will retry next cycle.
     */
    private void triggerShipmentRegistration(Order order) {
        try {
            BigDecimal fee = order.getShippingFee() == null ? null : order.getShippingFee().amount();
            shippingGateway.createAndRegister(order.getOrderId(), order.getMerchantId(),
                    order.getUserId(), order.getShippingAddress(), null, fee, order.getCurrency());
        } catch (Exception ex) {
            log.warn("Auto-shipment registration failed for order {}: {}",
                    order.getOrderId(), ex.getMessage());
        }
    }

    public OrderResult cancel(CancelOrderCommand command) {
        Order order = ownedByUser(command.orderId(), command.userId());
        if (order.getStatus().isPickedUpByCarrier()) {
            throw new OrderingException(OrderingErrorCode.ORDER_ALREADY_PICKED_UP);
        }
        order.cancel("USER_CANCELLED", command.reason());
        releaseReservationsBestEffort(order, "order-cancelled");
        releaseVoucherBestEffort(order, "order-cancelled");
        if (order.getPaymentId() != null) {
            paymentGateway.refund(order.getPaymentId(), order.getTotalAmount().amount(),
                    order.getCurrency(), "user cancellation");
        }
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCancelled(saved.getOrderId(), "USER_CANCELLED",
                command.reason(), OrderingIntegrationEventPublisherPort.CancellationKind.USER_CANCELLED);
        return mapper.toResult(saved);
    }

    /**
     * Cancel a PENDING order whose online payment failed. Idempotent: if the
     * order is already cancelled or approved (race with capture event), no-op.
     */
    public OrderResult cancelOnPaymentFailure(String orderId, String errorCode, String reason) {
        Order order = required(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Skipping payment-failure cancel for order {}: status is {}",
                    orderId, order.getStatus());
            return mapper.toResult(order);
        }
        order.autoCancel("PAYMENT_FAILED");
        releaseReservationsBestEffort(order, "payment-failed");
        releaseVoucherBestEffort(order, "payment-failed");
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCancelled(saved.getOrderId(), "PAYMENT_FAILED",
                reason == null ? errorCode : reason,
                OrderingIntegrationEventPublisherPort.CancellationKind.AUTO_CANCELLED);
        return mapper.toResult(saved);
    }

    public OrderResult rejectByMerchant(RejectOrderCommand command) {
        String merchantId = requireMerchantIdForOwner(command.ownerId());
        Order order = ownedByMerchant(command.orderId(), merchantId);
        order.rejectByMerchant(merchantId, command.reason());
        releaseReservationsBestEffort(order, "merchant-rejected");
        releaseVoucherBestEffort(order, "merchant-rejected");
        if (order.getPaymentId() != null) {
            paymentGateway.refund(order.getPaymentId(), order.getTotalAmount().amount(),
                    order.getCurrency(), "merchant rejected");
        }
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCancelled(saved.getOrderId(), "MERCHANT_REJECTED",
                command.reason(), OrderingIntegrationEventPublisherPort.CancellationKind.MERCHANT_REJECTED);
        return mapper.toResult(saved);
    }

    private void releaseVoucherBestEffort(Order order, String reason) {
        try {
            voucherGateway.release(order.getUserId(), order.getOrderId(), reason);
        } catch (RuntimeException ex) {
            log.warn("Voucher release for order {} ({}) failed: {}",
                    order.getOrderId(), reason, ex.getMessage());
        }
    }

    private void releaseReservationsBestEffort(Order order, String reason) {
        for (OrderItem item : order.items()) {
            if (item.reservationId() == null) {
                continue;
            }
            try {
                stockReservationGateway.release(item.reservationId(), reason);
            } catch (RuntimeException ex) {
                log.warn("Release of reservation {} ({}) failed: {}",
                        item.reservationId(), reason, ex.getMessage());
            }
        }
    }

    public OrderResult changeShippingInfo(ChangeShippingInfoCommand command) {
        Order order = ownedByUser(command.orderId(), command.userId());
        BigDecimal feeOverride = command.newShippingFee();
        Money newFee;
        if (feeOverride == null) {
            ShippingGateway.ShippingQuote quote = shippingGateway.quote(order.getOrderId(), order.getMerchantId(),
                    command.newAddress(), order.getCurrency());
            newFee = Money.of(quote.fee(), quote.currency());
        } else {
            newFee = Money.of(feeOverride, order.getCurrency());
        }
        order.changeShippingInfo(command.newAddress(), newFee);
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        return mapper.toResult(saved);
    }

    public OrderResult markShipped(ConfirmShippedCommand command) {
        Order order = required(command.orderId());
        order.markShipped(command.shipmentId());
        for (OrderItem item : order.items()) {
            if (item.reservationId() == null) {
                continue;
            }
            stockReservationGateway.commit(item.reservationId());
        }
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderShipped(saved.getOrderId(), command.shipmentId());
        return mapper.toResult(saved);
    }

    public OrderResult complete(ConfirmDeliveredCommand command) {
        Order order = required(command.orderId());
        order.complete();
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCompleted(saved.getOrderId());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public OrderResult getForRequester(String orderId, String requesterUserId) {
        Order order = required(orderId);
        if (order.getUserId().equals(requesterUserId)) {
            return mapper.toResult(order);
        }
        String requesterMerchantId = merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        if (requesterMerchantId != null && requesterMerchantId.equals(order.getMerchantId())) {
            return mapper.toResult(order);
        }
        throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<OrderResult> listByUser(String userId, int limit) {
        return orderRepository.findByUser(userId, limit).stream().map(mapper::toResult).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResult> listByUser(String userId, String statusFilter, int limit) {
        List<String> statuses = parseStatusFilter(statusFilter);
        if (statuses.isEmpty()) {
            return listByUser(userId, limit);
        }
        return orderRepository.findByUserAndStatuses(userId, statuses, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResult> listByMerchantOwner(String ownerId, String statusFilter, int limit) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        List<String> statuses = parseStatusFilter(statusFilter);
        if (statuses.isEmpty()) {
            return orderRepository.findByMerchant(merchantId, limit).stream()
                    .map(mapper::toResult)
                    .toList();
        }
        return orderRepository.findByMerchantAndStatuses(merchantId, statuses, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantOrderAnalyticsResult getMerchantAnalytics(String ownerId, LocalDate from, LocalDate to) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate safeTo = to == null ? LocalDate.now(zone) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(6) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.OrderAnalyticsRow> rows =
                orderRepository.findMerchantAnalyticsRows(merchantId, start, endExclusive);

        Map<LocalDate, DailyRevenueAccumulator> revenueByDate = new LinkedHashMap<>();
        for (LocalDate day = safeFrom; !day.isAfter(safeTo); day = day.plusDays(1)) {
            revenueByDate.put(day, new DailyRevenueAccumulator());
        }

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }

        String currency = "VND";
        long completedOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (OrderPersistencePort.OrderAnalyticsRow row : rows) {
            if (row.currency() != null && !row.currency().isBlank()) {
                currency = row.currency();
            }
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
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate safeTo = to == null ? LocalDate.now(zone) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.PlatformAnalyticsRow> rows =
                orderRepository.findPlatformAnalyticsRows(start, endExclusive);

        Map<LocalDate, DailyRevenueAccumulator> revenueByDate = new LinkedHashMap<>();
        for (LocalDate day = safeFrom; !day.isAfter(safeTo); day = day.plusDays(1)) {
            revenueByDate.put(day, new DailyRevenueAccumulator());
        }

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }

        Map<String, MerchantAccumulator> byMerchant = new LinkedHashMap<>();
        String currency = "VND";
        long completedOrders = 0;
        BigDecimal totalGmv = BigDecimal.ZERO;

        for (OrderPersistencePort.PlatformAnalyticsRow row : rows) {
            if (row.currency() != null && !row.currency().isBlank()) {
                currency = row.currency();
            }
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
                .limit(10)
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
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate safeTo = to == null ? LocalDate.now(zone) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);

        var start = safeFrom.atStartOfDay(zone).toInstant();
        var endExclusive = safeTo.plusDays(1).atStartOfDay(zone).toInstant();
        List<OrderPersistencePort.TopProductRow> rows =
                orderRepository.findMerchantTopProductRows(merchantId, start, endExclusive);

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
        return java.util.Arrays.stream(statusFilter.split(","))
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
        return orderRepository.findById(orderId)
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

    private String requireMerchantIdForOwner(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                        "No merchant registered for the authenticated user"));
    }

    private void releaseReservations(List<StockReservationGateway.Reservation> reservations, String reason) {
        for (StockReservationGateway.Reservation r : reservations) {
            try {
                stockReservationGateway.release(r.reservationId(), reason);
            } catch (Exception ex) {
                log.warn("Failed to release reservation {} during compensation: {}", r.reservationId(),
                        ex.getMessage());
            }
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
