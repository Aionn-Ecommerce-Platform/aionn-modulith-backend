package com.aionn.ordering.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.ordering.domain.event.ReturnEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.ordering.domain.valueobject.ReturnReasonType;
import com.aionn.ordering.domain.valueobject.ReturnRefundStatus;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class OrderReturn extends AggregateRoot {

    private final String returnId;
    private final String orderId;
    private final String userId;
    private final String merchantId;
    private final ReturnReasonType reasonType;
    private final String reason;
    private final String evidenceUrl;
    private Money refundAmount;
    private String returnWarehouseId;
    private String itemCondition;
    private String rejectionReason;
    private ReturnStatus status;
    private final Instant requestedAt;
    private Instant decidedAt;
    private Instant receivedAt;
    private ReturnRefundStatus refundStatus;
    private int refundAttempts;
    private String refundFailureReason;
    private Instant nextRefundAttemptAt;

    public OrderReturn(
            String returnId,
            String orderId,
            String userId,
            String merchantId,
            String reason,
            String evidenceUrl,
            Money refundAmount,
            String returnWarehouseId,
            String itemCondition,
            String rejectionReason,
            ReturnStatus status,
            Instant requestedAt,
            Instant decidedAt,
            Instant receivedAt) {
        this(returnId, orderId, userId, merchantId, ReturnReasonType.OTHER, reason, evidenceUrl,
                refundAmount, returnWarehouseId, itemCondition, rejectionReason, status, requestedAt,
                decidedAt, receivedAt,
                refundAmount == null ? ReturnRefundStatus.NOT_REQUIRED : ReturnRefundStatus.PENDING,
                0, null, null);
    }

    public OrderReturn(
            String returnId, String orderId, String userId, String merchantId,
            ReturnReasonType reasonType, String reason, String evidenceUrl, Money refundAmount,
            String returnWarehouseId, String itemCondition, String rejectionReason,
            ReturnStatus status, Instant requestedAt, Instant decidedAt, Instant receivedAt,
            ReturnRefundStatus refundStatus, int refundAttempts, String refundFailureReason,
            Instant nextRefundAttemptAt) {
        this.returnId = returnId;
        this.orderId = orderId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.reasonType = reasonType == null ? ReturnReasonType.OTHER : reasonType;
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
        this.refundAmount = refundAmount;
        this.returnWarehouseId = returnWarehouseId;
        this.itemCondition = itemCondition;
        this.rejectionReason = rejectionReason;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
        this.receivedAt = receivedAt;
        this.refundStatus = refundStatus;
        this.refundAttempts = refundAttempts;
        this.refundFailureReason = refundFailureReason;
        this.nextRefundAttemptAt = nextRefundAttemptAt;
    }

    public static OrderReturn request(
            String returnId,
            String orderId,
            String userId,
            String merchantId,
            ReturnReasonType reasonType,
            String reason,
            String evidenceUrl,
            Instant now) {
        Guard.require(reasonType != null,
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "reasonType must not be null"));
        Guard.require(reason != null && !reason.isBlank(),
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "reason must not be blank"));
        OrderReturn r = new OrderReturn(returnId, orderId, userId, merchantId, reasonType, reason,
                evidenceUrl, null, null, null, null, ReturnStatus.REQUESTED, now, null, null,
                ReturnRefundStatus.NOT_REQUIRED, 0, null, null);
        r.registerEvent(new ReturnEvents.ReturnRequested(returnId, orderId, reasonType, reason, evidenceUrl, now));
        return r;
    }

    public List<String> evidenceUrls() {
        if (evidenceUrl == null || evidenceUrl.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(evidenceUrl.split("\n"))
                .filter(url -> !url.isBlank())
                .toList();
    }

    public void approve(Money refundAmount, String returnWarehouseId, Instant now) {
        ensureTransition(ReturnStatus.APPROVED);
        Guard.require(refundAmount != null,
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "refundAmount required"));
        this.refundAmount = refundAmount;
        this.returnWarehouseId = returnWarehouseId;
        this.status = ReturnStatus.APPROVED;
        this.decidedAt = now;
        this.refundStatus = ReturnRefundStatus.PENDING;
        this.refundAttempts = 0;
        this.refundFailureReason = null;
        this.nextRefundAttemptAt = now;
        registerEvent(new ReturnEvents.ReturnApproved(returnId, orderId, merchantId,
                refundAmount.amount(), refundAmount.currency(), returnWarehouseId, decidedAt, decidedAt));
    }

    public void markRefunded(Instant now) {
        Guard.require(refundStatus == ReturnRefundStatus.PENDING || refundStatus == ReturnRefundStatus.FAILED,
                () -> new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE));
        refundStatus = ReturnRefundStatus.REFUNDED;
        refundFailureReason = null;
        nextRefundAttemptAt = null;
    }

    public void markRefundFailed(String reason, Instant nextAttemptAt) {
        Guard.require(refundStatus == ReturnRefundStatus.PENDING || refundStatus == ReturnRefundStatus.FAILED,
                () -> new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE));
        refundStatus = ReturnRefundStatus.FAILED;
        refundAttempts++;
        refundFailureReason = reason;
        this.nextRefundAttemptAt = nextAttemptAt;
    }

    public boolean refundCanBeAttempted(Instant now, int maxAttempts) {
        return refundStatus != ReturnRefundStatus.REFUNDED
                && refundStatus != ReturnRefundStatus.NOT_REQUIRED
                && refundAttempts < maxAttempts
                && (nextRefundAttemptAt == null || !nextRefundAttemptAt.isAfter(now));
    }

    public void confirmReceived(String itemCondition, Instant now) {
        ensureTransition(ReturnStatus.ITEM_RECEIVED);
        this.itemCondition = itemCondition;
        this.status = ReturnStatus.ITEM_RECEIVED;
        this.receivedAt = now;
        registerEvent(new ReturnEvents.ReturnItemReceived(
                returnId, orderId, merchantId, itemCondition, receivedAt, receivedAt));
    }

    public void reject(String rejectionReason, Instant now) {
        Guard.require(refundStatus != ReturnRefundStatus.REFUNDED,
                () -> new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE,
                        "A refunded return cannot be rejected"));
        ensureTransition(ReturnStatus.REJECTED);
        this.rejectionReason = rejectionReason;
        this.status = ReturnStatus.REJECTED;
        this.decidedAt = now;
        registerEvent(new ReturnEvents.ReturnRejected(returnId, orderId, merchantId, rejectionReason,
                decidedAt, decidedAt));
    }

    public void cancel(Instant now) {
        ensureTransition(ReturnStatus.CANCELLED);
        this.status = ReturnStatus.CANCELLED;
        this.decidedAt = now;
        registerEvent(new ReturnEvents.ReturnCancelled(returnId, orderId, userId, now, now));
    }

    private void ensureTransition(ReturnStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new OrderingException(OrderingErrorCode.RETURN_INVALID_STATE,
                        "Cannot transition return from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return returnId;
    }
}
