package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderReturnService {

    private static final Duration RETURN_WINDOW = Duration.ofDays(7);

    private final OrderPersistencePort orderRepository;
    private final OrderReturnPersistencePort returnRepository;
    private final OrderingResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final PaymentGateway paymentGateway;

    public ReturnResult requestReturn(RequestReturnCommand command) {
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
        if (Instant.now().isAfter(completedAt.plus(RETURN_WINDOW))) {
            throw new OrderingException(OrderingErrorCode.ORDER_RETURN_WINDOW_EXPIRED);
        }
        OrderReturn r = OrderReturn.request(IdGenerator.ulid(), order.getOrderId(),
                order.getUserId(), order.getMerchantId(), command.reason(), command.evidenceUrl());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return mapper.toResult(saved);
    }

    public ReturnResult approve(ApproveReturnCommand command) {
        OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
        Money refundAmount = command.refundAmount() == null
                ? null
                : Money.of(command.refundAmount(), command.currency() == null ? "VND" : command.currency());
        r.approve(refundAmount, command.returnWarehouseId());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        triggerRefundIfPaid(saved, "return approved");
        return mapper.toResult(saved);
    }

    public ReturnResult reject(RejectReturnCommand command) {
        OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
        r.reject(command.reason());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return mapper.toResult(saved);
    }

    public ReturnResult confirmItemReceived(ConfirmItemReceivedCommand command) {
        OrderReturn r = ownedByOwner(command.returnId(), command.ownerId());
        r.confirmReceived(command.itemCondition());
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public ReturnResult getForRequester(String returnId, String requesterUserId) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        if (r.getUserId().equals(requesterUserId)) {
            return mapper.toResult(r);
        }
        String requesterMerchantId = merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        if (requesterMerchantId != null && requesterMerchantId.equals(r.getMerchantId())) {
            return mapper.toResult(r);
        }
        throw new OrderingException(OrderingErrorCode.ORDER_FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReturnResult> listMine(String userId, int limit) {
        return returnRepository.findByUserId(userId, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ReturnResult> listMerchant(String userId, int limit) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(userId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                        "No merchant registered for the authenticated user"));
        return returnRepository.findByMerchantId(merchantId, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ReturnResult> adminListByStatus(
            com.aionn.ordering.domain.valueobject.ReturnStatus status, int limit) {
        return returnRepository.findByStatus(status, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnResult adminGet(String returnId) {
        return mapper.toResult(returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public ReturnAnalyticsResult adminAnalytics(java.time.LocalDate from, java.time.LocalDate to) {
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.LocalDate safeTo = to == null ? java.time.LocalDate.now(zone) : to;
        java.time.LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new OrderingException(OrderingErrorCode.INVALID_ARGUMENT);
        }
        Instant start = safeFrom.atStartOfDay(zone).toInstant();
        Instant end = safeTo.plusDays(1).atStartOfDay(zone).toInstant();

        var rows = returnRepository.findReturnAnalyticsRows(start, end);
        long completedOrders = returnRepository.countCompletedOrdersBetween(start, end);

        java.util.Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> byReason = new java.util.LinkedHashMap<>();
        java.math.BigDecimal refundTotal = java.math.BigDecimal.ZERO;
        String currency = "VND";
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

    public ReturnResult adminApprove(String returnId, java.math.BigDecimal refundAmount,
            String currency, String returnWarehouseId) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        Money refund = refundAmount == null
                ? null
                : Money.of(refundAmount, currency == null ? "VND" : currency);
        r.approve(refund, returnWarehouseId);
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        triggerRefundIfPaid(saved, "return approved (admin)");
        return mapper.toResult(saved);
    }

    private void triggerRefundIfPaid(OrderReturn r, String reason) {
        if (r.getRefundAmount() == null) {
            return;
        }
        Order order = orderRepository.findById(r.getOrderId()).orElse(null);
        if (order == null || order.getPaymentId() == null) {
            return;
        }
        try {
            paymentGateway.refund(order.getPaymentId(), r.getRefundAmount().amount(),
                    r.getRefundAmount().currency(), reason);
        } catch (RuntimeException ex) {
            log.error("Refund for return {} (order {}) failed", r.getReturnId(), r.getOrderId(), ex);
            throw ex;
        }
    }

    public ReturnResult adminReject(String returnId, String reason) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        r.reject(reason);
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return mapper.toResult(saved);
    }

    public ReturnResult adminConfirmItemReceived(String returnId, String itemCondition) {
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        r.confirmReceived(itemCondition);
        OrderReturn saved = returnRepository.save(r);
        eventPublisher.publish(r.pullEvents());
        return mapper.toResult(saved);
    }

    private OrderReturn ownedByOwner(String returnId, String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT,
                        "No merchant registered for the authenticated user"));
        OrderReturn r = returnRepository.findById(returnId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.RETURN_NOT_FOUND));
        if (!r.getMerchantId().equals(merchantId)) {
            throw new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT);
        }
        return r;
    }
}
