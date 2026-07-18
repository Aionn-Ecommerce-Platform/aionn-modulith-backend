package com.aionn.ordering.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.ordering.domain.event.ReturnEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class OrderReturn extends AggregateRoot {

    private final String returnId;
    private final String orderId;
    private final String userId;
    private final String merchantId;
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
        this.returnId = returnId;
        this.orderId = orderId;
        this.userId = userId;
        this.merchantId = merchantId;
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
    }

    public static OrderReturn request(
            String returnId,
            String orderId,
            String userId,
            String merchantId,
            String reason,
            String evidenceUrl) {
        Guard.require(reason != null && !reason.isBlank(),
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "reason must not be blank"));
        Instant now = Instant.now();
        OrderReturn r = new OrderReturn(returnId, orderId, userId, merchantId, reason, evidenceUrl,
                null, null, null, null, ReturnStatus.REQUESTED, now, null, null);
        r.registerEvent(new ReturnEvents.ReturnRequested(returnId, orderId, reason, evidenceUrl, now));
        return r;
    }

    public void approve(Money refundAmount, String returnWarehouseId) {
        ensureTransition(ReturnStatus.APPROVED);
        Guard.require(refundAmount != null,
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "refundAmount required"));
        this.refundAmount = refundAmount;
        this.returnWarehouseId = returnWarehouseId;
        this.status = ReturnStatus.APPROVED;
        this.decidedAt = Instant.now();
        registerEvent(new ReturnEvents.ReturnApproved(returnId, orderId, merchantId,
                refundAmount.amount(), refundAmount.currency(), returnWarehouseId, decidedAt, decidedAt));
    }

    public void confirmReceived(String itemCondition) {
        ensureTransition(ReturnStatus.ITEM_RECEIVED);
        this.itemCondition = itemCondition;
        this.status = ReturnStatus.ITEM_RECEIVED;
        this.receivedAt = Instant.now();
        registerEvent(new ReturnEvents.ReturnItemReceived(
                returnId, orderId, merchantId, itemCondition, receivedAt, receivedAt));
    }

    public void reject(String rejectionReason) {
        ensureTransition(ReturnStatus.REJECTED);
        this.rejectionReason = rejectionReason;
        this.status = ReturnStatus.REJECTED;
        this.decidedAt = Instant.now();
        registerEvent(new ReturnEvents.ReturnRejected(returnId, orderId, merchantId, rejectionReason,
                decidedAt, decidedAt));
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
