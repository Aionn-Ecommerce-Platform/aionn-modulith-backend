package com.aionn.ordering.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.ordering.domain.event.OrderEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Order extends AggregateRoot {

    private final String orderId;
    private final String parentOrderId;
    private final String userId;
    private final String merchantId;
    private final String proposalId;
    private String paymentMethodId;
    private final String currency;
    private final List<OrderItem> items = new ArrayList<>();
    private ShippingAddress shippingAddress;
    private Money shippingFee;
    private Money totalAmount;
    private OrderStatus status;
    private String paymentId;
    private String reasonCode;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    public Order(
            String orderId,
            String parentOrderId,
            String userId,
            String merchantId,
            String proposalId,
            String paymentMethodId,
            String currency,
            List<OrderItem> items,
            ShippingAddress shippingAddress,
            Money shippingFee,
            Money totalAmount,
            OrderStatus status,
            String paymentId,
            String reasonCode,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            Instant cancelledAt) {
        this.orderId = orderId;
        this.parentOrderId = parentOrderId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.proposalId = proposalId;
        this.paymentMethodId = paymentMethodId;
        this.currency = currency;
        if (items != null)
            this.items.addAll(items);
        this.shippingAddress = shippingAddress;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentId = paymentId;
        this.reasonCode = reasonCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    public static Order place(
            String orderId,
            String userId,
            String merchantId,
            String proposalId,
            String paymentMethodId,
            String currency,
            List<OrderItem> items,
            ShippingAddress address,
            Money shippingFee,
            Money merchandiseSubtotal) {
        Guard.require(items != null && !items.isEmpty(),
                () -> new OrderingException(OrderingErrorCode.CART_EMPTY));
        Money originalLineSubtotal = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money.zero(currency), Money::add);
        Money lineSubtotal = merchandiseSubtotal == null ? originalLineSubtotal : merchandiseSubtotal;
        Money totalAmount = shippingFee == null ? lineSubtotal : lineSubtotal.add(shippingFee);

        Instant now = Instant.now();
        Order order = new Order(orderId, null, userId, merchantId, proposalId, paymentMethodId, currency,
                items, address, shippingFee, totalAmount,
                OrderStatus.PENDING, null, null, now, now, null, null);
        order.registerEvent(new OrderEvents.OrderPlaced(
                orderId, userId, merchantId, proposalId,
                items.stream().map(it -> new OrderEvents.OrderLineSnapshot(
                        it.skuId(), it.qty(), it.unitPrice().amount(), it.warehouseId(), it.reservationId()))
                        .toList(),
                totalAmount.amount(),
                currency,
                address == null ? null : address.addressId(),
                paymentMethodId,
                now, now));
        return order;
    }

    public void approve(String paymentId) {
        ensureTransition(OrderStatus.APPROVED);
        this.status = OrderStatus.APPROVED;
        this.paymentId = paymentId;
        touch();
        registerEvent(new OrderEvents.OrderApproved(orderId, paymentId, updatedAt, updatedAt));
    }

    public void confirmPreparation() {
        Guard.require(status == OrderStatus.APPROVED,
                () -> new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Order must be APPROVED before merchant can prepare"));
        this.status = OrderStatus.PREPARING;
        touch();
        registerEvent(new OrderEvents.OrderPreparationConfirmed(orderId, merchantId, updatedAt, updatedAt));
    }

    public void markShipped(String shipmentId) {
        Guard.require(status == OrderStatus.PREPARING,
                () -> new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Order must be PREPARING to be shipped"));
        this.status = OrderStatus.SHIPPED;
        touch();
        registerEvent(new OrderEvents.OrderShipped(orderId, shipmentId, updatedAt, updatedAt));
    }

    public void complete() {
        Guard.require(status == OrderStatus.SHIPPED,
                () -> new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Only SHIPPED orders can be completed"));
        this.status = OrderStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = completedAt;
        registerEvent(new OrderEvents.OrderCompleted(orderId, completedAt, completedAt));
    }

    public void cancel(String reasonCode, String reason) {
        ensureTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.reasonCode = reasonCode;
        this.cancelledAt = Instant.now();
        this.updatedAt = cancelledAt;
        registerEvent(new OrderEvents.OrderCancelled(orderId, reasonCode, reason, cancelledAt, cancelledAt));
    }

    public void autoCancel(String reasonCode) {
        ensureTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.reasonCode = reasonCode;
        this.cancelledAt = Instant.now();
        this.updatedAt = cancelledAt;
        registerEvent(new OrderEvents.OrderAutoCancelled(orderId, reasonCode, cancelledAt, cancelledAt));
    }

    public void rejectByMerchant(String merchantId, String reason) {
        Guard.require(this.merchantId.equals(merchantId),
                () -> new OrderingException(OrderingErrorCode.ORDER_NOT_OWNED_BY_MERCHANT));
        ensureTransition(OrderStatus.REJECTED);
        this.status = OrderStatus.REJECTED;
        this.reasonCode = "MERCHANT_REJECTED";
        this.cancelledAt = Instant.now();
        this.updatedAt = cancelledAt;
        registerEvent(new OrderEvents.OrderRejectedByMerchant(orderId, merchantId, reason, cancelledAt, cancelledAt));
    }

    public void changeShippingInfo(ShippingAddress newAddress, Money newShippingFee) {
        Guard.require(!status.isPickedUpByCarrier() && !status.isTerminal(),
                () -> new OrderingException(OrderingErrorCode.ORDER_ALREADY_PICKED_UP));
        BigDecimal feeAmount = newShippingFee == null ? null : newShippingFee.amount();
        Money oldShipping = this.shippingFee == null ? Money.zero(currency) : this.shippingFee;
        Money lineSubtotal = totalAmount.subtract(oldShipping);
        Money newTotal = newShippingFee == null ? lineSubtotal : lineSubtotal.add(newShippingFee);

        this.shippingAddress = newAddress;
        this.shippingFee = newShippingFee;
        this.totalAmount = newTotal;
        touch();
        registerEvent(new OrderEvents.OrderShippingInfoChanged(
                orderId, newAddress, feeAmount, currency, updatedAt, updatedAt));
    }

    public List<OrderItem> items() {
        return Collections.unmodifiableList(items);
    }

    private void ensureTransition(OrderStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new OrderingException(OrderingErrorCode.ORDER_INVALID_STATE,
                        "Cannot transition order from " + status + " to " + next));
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    protected String aggregateId() {
        return orderId;
    }
}
