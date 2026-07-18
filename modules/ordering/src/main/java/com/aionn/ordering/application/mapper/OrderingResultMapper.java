package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.model.OrderReturn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderingResultMapper {

    public CartResult toResult(Cart cart) {
        return new CartResult(
                cart.getCartId(),
                cart.getUserId(),
                cart.snapshot().stream()
                        .map(e -> new CartResult.CartItemResult(e.getKey(), e.getValue()))
                        .toList(),
                cart.getVoucherCode(),
                cart.getCreatedAt(),
                cart.getUpdatedAt());
    }

    public OrderResult toResult(Order order) {
        return new OrderResult(
                order.getOrderId(),
                order.getParentOrderId(),
                order.getUserId(),
                order.getMerchantId(),
                order.getProposalId(),
                order.getPaymentMethodId(),
                order.getPaymentId(),
                order.getCurrency(),
                order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount().amount(),
                order.getShippingFee() == null ? null : order.getShippingFee().amount(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().addressId(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().fullName(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().phone(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().addressLine(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().wardCode(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().districtCode(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().provinceCode(),
                order.getShippingAddress() == null ? null : order.getShippingAddress().countryCode(),
                order.items().stream()
                        .map(this::toItemResult)
                        .toList(),
                order.getStatus().name(),
                order.getReasonCode(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt());
    }

    public ReturnResult toResult(OrderReturn r) {
        return new ReturnResult(
                r.getReturnId(),
                r.getOrderId(),
                r.getUserId(),
                r.getMerchantId(),
                r.getReason(),
                r.getEvidenceUrl(),
                r.getRefundAmount() == null ? null : r.getRefundAmount().amount(),
                r.getRefundAmount() == null ? null : r.getRefundAmount().currency(),
                r.getReturnWarehouseId(),
                r.getItemCondition(),
                r.getRejectionReason(),
                r.getStatus().name(),
                r.getRequestedAt(),
                r.getDecidedAt(),
                r.getReceivedAt());
    }

    private OrderResult.OrderItemResult toItemResult(OrderItem item) {
        return new OrderResult.OrderItemResult(
                item.skuId(), item.qty(), item.unitPrice().amount(),
                item.warehouseId(), item.reservationId());
    }
}

