package com.aionn.ordering.application.service;

import com.aionn.ordering.domain.valueobject.ReturnRefundStatus;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnItemPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.policy.OrderDefaultsPolicy;
import com.aionn.ordering.application.policy.OrderingAnalyticsPolicy;
import com.aionn.ordering.application.policy.RefundRetryPolicy;
import com.aionn.ordering.application.policy.ReturnWindowPolicy;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.util.RetryBackoff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderReturnService {

    private final OrderPersistencePort orderRepository;
    private final OrderReturnPersistencePort returnRepository;
    private final OrderReturnItemPersistencePort returnItemRepository;
    private final EventPublisher eventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final ReturnWindowPolicy returnWindowPolicy;
    private final RefundRetryPolicy refundRetryPolicy;
    private final OrderDefaultsPolicy orderDefaultsPolicy;
    private final OrderingAnalyticsPolicy analyticsPolicy;
    private final PaymentGateway paymentGateway;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public OrderReturn requestReturn(RequestReturnCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(command.userId())) {
            throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                    "Returns are only allowed on COMPLETED orders");
        }
        Instant completedAt = Objects.requireNonNull(order.getCompletedAt(),
                "completed order must have a completedAt timestamp");
        Instant now = clock.instant();
        if (!returnWindowPolicy.isWithinWindow(completedAt, now)) {
            throw new OrderingException(OrderingErrorCode.ORDER_RETURN_WINDOW_EXPIRED);
        }
        if (returnRepository.hasActiveReturn(order.getOrderId())) {
            throw new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE,
                    "This order already has an active return request");
        }
        OrderReturn r = OrderReturn.request(IdGenerator.ulid(), order.getOrderId(),
                order.getUserId(), order.getMerchantId(), command.reason(), command.evidenceUrl(), now);
        OrderReturn saved = returnRepository.save(r);
        saveSelectedItems(saved.getReturnId(), order, command.items());
        eventPublisher.publish(r.pullEvents());
        return saved;
    }

    private void saveSelectedItems(String returnId, Order order,
            java.util.List<RequestReturnCommand.Item> requested) {
        if (requested == null || requested.isEmpty()) {
            return;
        }
        java.util.Map<String, OrderItem> orderItems = order.items().stream()
                .collect(java.util.stream.Collectors.toMap(OrderItem::skuId, item -> item));
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<OrderReturnItemPersistencePort.Item> items = new java.util.ArrayList<>();
        for (RequestReturnCommand.Item line : requested) {
            if (line == null || line.skuId() == null || line.skuId().isBlank() || line.qty() <= 0) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Return items require a skuId and a positive quantity");
            }
            if (!seen.add(line.skuId())) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Duplicate return item: " + line.skuId());
            }
            OrderItem ordered = orderItems.get(line.skuId());
            if (ordered == null) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Item is not part of this order: " + line.skuId());
            }
            if (line.qty() > ordered.qty()) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Return quantity exceeds ordered quantity for " + line.skuId());
            }
            java.math.BigDecimal unit = ordered.unitPrice().amount();
            items.add(new OrderReturnItemPersistencePort.Item(
                    line.skuId(), line.qty(), unit,
                    unit.multiply(java.math.BigDecimal.valueOf(line.qty()))));
        }
        returnItemRepository.saveAll(returnId, items);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderReturn approve(ApproveReturnCommand command) {
        OrderReturn saved = transactionTemplate.execute(status -> {
            OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
            Order order = orderRepository.findById(r.getOrderId())
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
            String currency = command.currency() == null
                    ? order.getTotalAmount().currency() : command.currency();
            if (!currency.equalsIgnoreCase(order.getTotalAmount().currency())) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Refund currency must match the order currency");
            }
            Money refundAmount = command.refundAmount() == null
                    ? null
                    : Money.of(command.refundAmount(), currency);
            if (refundAmount != null
                    && refundAmount.amount().compareTo(order.getTotalAmount().amount()) > 0) {
                throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Refund amount exceeds the order total");
            }
            if (r.getStatus() == ReturnStatus.APPROVED) {
                ensureSameApproval(r, refundAmount, command.returnWarehouseId());
                return r;
            }
            r.approve(refundAmount, command.returnWarehouseId(), clock.instant());
            OrderReturn persisted = returnRepository.save(r);
            eventPublisher.publish(r.pullEvents());
            return persisted;
        });
        return attemptRefund(saved.getReturnId(), "return approved");
    }

    public OrderReturn reject(RejectReturnCommand command) {
        OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
        r.reject(command.reason(), clock.instant());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return saved;
    }

    public OrderReturn confirmItemReceived(ConfirmItemReceivedCommand command) {
        OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
        r.confirmReceived(command.itemCondition(), clock.instant());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public OrderReturn getForRequester(String returnId, String requesterUserId) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        if (r.getUserId().equals(requesterUserId)) {
            return r;
        }
        String requesterMerchantId = merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        if (requesterMerchantId != null && requesterMerchantId.equals(r.getMerchantId())) {
            return r;
        }
        throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<OrderReturnItemPersistencePort.Item> listSelectedItems(String returnId,
            String requesterUserId) {
        getForRequester(returnId, requesterUserId);
        return returnItemRepository.findByReturnId(returnId);
    }

    @Transactional(readOnly = true)
    public List<OrderReturn> listMine(String userId, int limit) {
        return returnRepository.findByUserId(userId, limit);
    }

    @Transactional(readOnly = true)
    public List<OrderReturn> listMerchant(String merchantId, int limit) {
        return returnRepository.findByMerchantId(merchantId, limit);
    }

    @Transactional(readOnly = true)
    public List<OrderReturn> adminListByStatus(
            ReturnStatus status, int limit) {
        return returnRepository.findByStatus(status, limit);
    }

    @Transactional(readOnly = true)
    public OrderReturn adminGet(String returnId) {
        return returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ReturnAnalyticsResult adminAnalytics(LocalDate from, LocalDate to) {
        ZoneId zone = analyticsPolicy.zone();
        LocalDate safeTo = to == null ? clock.instant().atZone(zone).toLocalDate() : to;
        LocalDate safeFrom = from == null
                ? safeTo.minusDays(analyticsPolicy.platformDefaultRangeDays() - 1L)
                : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }
        Instant start = safeFrom.atStartOfDay(zone).toInstant();
        Instant end = safeTo.plusDays(1).atStartOfDay(zone).toInstant();

        var rows = returnRepository.findReturnAnalyticsRows(start, end);
        long completedOrders = returnRepository.countCompletedOrdersBetween(start, end);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byReason = new LinkedHashMap<>();
        BigDecimal refundTotal = BigDecimal.ZERO;
        String currency = orderDefaultsPolicy.defaultCurrency();
        for (var row : rows) {
            byStatus.merge(row.status(), 1L, Long::sum);
            String reason = (row.reason() == null || row.reason().isBlank()) ? "UNKNOWN" : row.reason();
            byReason.merge(reason, 1L, Long::sum);
            if (row.refundAmount() != null) {
                refundTotal = refundTotal.add(row.refundAmount());
                if (row.refundCurrency() != null && !row.refundCurrency().isBlank()) {
                    currency = row.refundCurrency();
                }
            }
        }
        double returnRate = completedOrders == 0 ? 0.0 : (double) rows.size() / completedOrders;
        var statusList = byStatus.entrySet().stream()
                .map(e -> new ReturnAnalyticsResult.StatusCount(e.getKey(), e.getValue()))
                .toList();
        var reasonList = byReason.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> new ReturnAnalyticsResult.ReasonCount(e.getKey(), e.getValue()))
                .toList();
        return new ReturnAnalyticsResult(safeFrom, safeTo, rows.size(), completedOrders,
                returnRate, refundTotal, currency, statusList, reasonList);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderReturn adminApprove(String returnId, BigDecimal refundAmount,
            String currency, String returnWarehouseId) {
        OrderReturn saved = transactionTemplate.execute(status -> {
            OrderReturn r = returnRepository.findById(returnId)
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
            Money refund = refundAmount == null
                    ? null
                    : Money.of(refundAmount,
                            currency == null ? orderDefaultsPolicy.defaultCurrency() : currency);
            if (r.getStatus() == ReturnStatus.APPROVED) {
                ensureSameApproval(r, refund, returnWarehouseId);
                return r;
            }
            r.approve(refund, returnWarehouseId, clock.instant());
            OrderReturn persisted = returnRepository.save(r);
            eventPublisher.publish(r.pullEvents());
            return persisted;
        });
        return attemptRefund(saved.getReturnId(), "return approved (admin)");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderReturn retryRefund(String returnId) {
        return attemptRefund(returnId, "return refund retry");
    }

    private OrderReturn attemptRefund(String returnId, String reason) {
        OrderReturn r = transactionTemplate.execute(status -> returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND)));
        if (!r.refundCanBeAttempted(clock.instant(), refundRetryPolicy.maxAttempts())) {
            return r;
        }
        if (r.getRefundAmount() == null) {
            return r;
        }
        RefundRequest refund = transactionTemplate.execute(status -> orderRepository.findById(r.getOrderId())
                .filter(order -> order.getPaymentId() != null)
                .map(order -> new RefundRequest(order.getPaymentId(), r.getRefundAmount().amount(),
                        r.getRefundAmount().currency(), reason))
                .orElse(null));
        if (refund == null) {
            return markRefunded(returnId);
        }
        try {
            paymentGateway.refund(refund.paymentId(), refund.amount(), refund.currency(), refund.reason(),
                    "return:" + r.getReturnId() + ":refund");
            return markRefunded(returnId);
        } catch (RuntimeException ex) {
            log.error("Refund for return {} (order {}) failed", r.getReturnId(), r.getOrderId(), ex);
            return markRefundFailed(returnId, ex);
        }
    }

    private OrderReturn markRefunded(String returnId) {
        return transactionTemplate.execute(status -> {
            OrderReturn current = returnRepository.findById(returnId)
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
            if (current.getRefundStatus() == ReturnRefundStatus.REFUNDED) {
                return current;
            }
            current.markRefunded(clock.instant());
            return returnRepository.save(current);
        });
    }

    private OrderReturn markRefundFailed(String returnId, RuntimeException failure) {
        return transactionTemplate.execute(status -> {
            OrderReturn current = returnRepository.findById(returnId)
                    .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
            long delaySeconds = RetryBackoff.seconds(current.getRefundAttempts());
            current.markRefundFailed(failure.getMessage(), clock.instant().plusSeconds(delaySeconds));
            return returnRepository.save(current);
        });
    }

    private record RefundRequest(String paymentId, BigDecimal amount, String currency, String reason) {}

    private void ensureSameApproval(OrderReturn existing, Money refund, String warehouseId) {
        boolean sameRefund = existing.getRefundAmount() != null && refund != null
                && existing.getRefundAmount().amount().compareTo(refund.amount()) == 0
                && existing.getRefundAmount().currency().equalsIgnoreCase(refund.currency());
        if (!sameRefund || !Objects.equals(existing.getReturnWarehouseId(), warehouseId)) {
            throw new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE,
                    "Approved return cannot be retried with different refund details");
        }
    }

    public OrderReturn adminReject(String returnId, String reason) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        r.reject(reason, clock.instant());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return saved;
    }

    public OrderReturn adminConfirmItemReceived(String returnId, String itemCondition) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        r.confirmReceived(itemCondition, clock.instant());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return saved;
    }

    private OrderReturn ownedByOwner(String returnId, String merchantId) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        if (!r.getMerchantId().equals(merchantId)) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT);
        }
        return r;
    }
}
