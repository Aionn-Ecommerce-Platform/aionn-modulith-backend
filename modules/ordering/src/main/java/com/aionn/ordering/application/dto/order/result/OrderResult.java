package com.aionn.ordering.application.dto.order.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResult(
        String orderId,
        String orderSn,
        String parentOrderId,
        String userId,
        String merchantId,
        String proposalId,
        String paymentMethodId,
        String paymentId,
        String currency,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        String addressId,
        String recipientName,
        String recipientPhone,
        String recipientAddressLine,
        String recipientWardCode,
        String recipientDistrictCode,
        String recipientProvinceCode,
        String recipientCountryCode,
        List<OrderItemResult> items,
        String status,
        String reasonCode,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Instant cancelledAt) {

    public OrderResult(
            String orderId,
            String parentOrderId,
            String userId,
            String merchantId,
            String proposalId,
            String paymentMethodId,
            String paymentId,
            String currency,
            BigDecimal totalAmount,
            BigDecimal shippingFee,
            String addressId,
            String recipientName,
            String recipientPhone,
            String recipientAddressLine,
            String recipientWardCode,
            String recipientDistrictCode,
            String recipientProvinceCode,
            String recipientCountryCode,
            List<OrderItemResult> items,
            String status,
            String reasonCode,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            Instant cancelledAt) {
        this(orderId, null, parentOrderId, userId, merchantId, proposalId, paymentMethodId, paymentId, currency,
                totalAmount, shippingFee, addressId, recipientName, recipientPhone, recipientAddressLine,
                recipientWardCode, recipientDistrictCode, recipientProvinceCode, recipientCountryCode,
                items, status, reasonCode, createdAt, updatedAt, completedAt, cancelledAt);
    }

    public record OrderItemResult(
            String skuId, int qty, BigDecimal unitPrice, String warehouseId, String reservationId) {
    }
}

