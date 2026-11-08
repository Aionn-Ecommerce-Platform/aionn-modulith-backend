package com.aionn.ucp.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain record representing an active or completed UCP checkout session.
 */
public record UcpCheckoutSession(
        String id,
        String userId,
        String cartId,
        String status,
        String currency,
        Map<String, Integer> items,
        Map<String, Object> buyer,
        Map<String, Object> context,
        String orderId,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Map<String, Long> priceSnapshot,
        Map<String, Object> payment,
        Map<String, Object> fulfillment,
        String voucherCode,
        long version) {

    public UcpCheckoutSession {
        items = items != null ? Collections.unmodifiableMap(new LinkedHashMap<>(items)) : Map.of();
        buyer = buyer != null ? Collections.unmodifiableMap(new LinkedHashMap<>(buyer)) : null;
        context = context != null ? Collections.unmodifiableMap(new LinkedHashMap<>(context)) : null;
        priceSnapshot = priceSnapshot != null ? Collections.unmodifiableMap(new LinkedHashMap<>(priceSnapshot)) : Map.of();
        payment = payment != null ? Collections.unmodifiableMap(new LinkedHashMap<>(payment)) : null;
        fulfillment = fulfillment != null ? Collections.unmodifiableMap(new LinkedHashMap<>(fulfillment)) : null;
    }

    /**
     * Backward-compatible constructor for callers predating the payment /
     * fulfillment / voucher fields — a session created this way carries none.
     */
    public UcpCheckoutSession(
            String id,
            String userId,
            String cartId,
            String status,
            String currency,
            Map<String, Integer> items,
            Map<String, Object> buyer,
            Map<String, Object> context,
            String orderId,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            Map<String, Long> priceSnapshot,
            long version) {
        this(id, userId, cartId, status, currency, items, buyer, context, orderId, createdAt, updatedAt, expiresAt, priceSnapshot, null, null, null, version);
    }

    public UcpCheckoutSession(
            String id,
            String userId,
            String cartId,
            String status,
            String currency,
            Map<String, Integer> items,
            Map<String, Object> buyer,
            Map<String, Object> context,
            String orderId,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            Map<String, Long> priceSnapshot) {
        this(id, userId, cartId, status, currency, items, buyer, context, orderId, createdAt, updatedAt, expiresAt, priceSnapshot, null, null, null, 1L);
    }

    public UcpCheckoutSession(
            String id,
            String userId,
            String cartId,
            String status,
            String currency,
            Map<String, Integer> items,
            Map<String, Object> buyer,
            Map<String, Object> context,
            String orderId,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt) {
        this(id, userId, cartId, status, currency, items, buyer, context, orderId, createdAt, updatedAt, expiresAt, Map.of(), null, null, null, 1L);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isCanceled() {
        return "canceled".equalsIgnoreCase(status);
    }

    public boolean isCompleting() {
        return "completing".equalsIgnoreCase(status);
    }

    public UcpCheckoutSession withCompleting(Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "completing",
                currency,
                items,
                buyer,
                context,
                orderId,
                createdAt,
                now,
                expiresAt,
                priceSnapshot,
                payment,
                fulfillment,
                voucherCode,
                version + 1);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now) {
        return withUpdatedItems(newItems, newBuyer, newContext, newCurrency, now, priceSnapshot);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now, Map<String, Long> newPriceSnapshot) {
        return withUpdatedItems(newItems, newBuyer, newContext, newCurrency, now, newPriceSnapshot, payment, fulfillment,
                voucherCode);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now, Map<String, Long> newPriceSnapshot,
            Map<String, Object> newPayment, Map<String, Object> newFulfillment) {
        return withUpdatedItems(newItems, newBuyer, newContext, newCurrency, now, newPriceSnapshot, newPayment,
                newFulfillment, voucherCode);
    }

    public UcpCheckoutSession withUpdatedItems(Map<String, Integer> newItems, Map<String, Object> newBuyer,
            Map<String, Object> newContext, String newCurrency, Instant now, Map<String, Long> newPriceSnapshot,
            Map<String, Object> newPayment, Map<String, Object> newFulfillment, String newVoucherCode) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "incomplete",
                newCurrency != null ? newCurrency : currency,
                newItems,
                newBuyer != null ? newBuyer : buyer,
                newContext != null ? newContext : context,
                orderId,
                createdAt,
                now,
                expiresAt,
                newPriceSnapshot != null ? newPriceSnapshot : priceSnapshot,
                newPayment != null ? newPayment : payment,
                newFulfillment != null ? newFulfillment : fulfillment,
                newVoucherCode != null ? newVoucherCode : voucherCode,
                version + 1);
    }

    public UcpCheckoutSession withPaymentAndFulfillment(Map<String, Object> newPayment,
            Map<String, Object> newFulfillment, Instant now) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                status,
                currency,
                items,
                buyer,
                context,
                orderId,
                createdAt,
                now,
                expiresAt,
                priceSnapshot,
                newPayment != null ? newPayment : payment,
                newFulfillment != null ? newFulfillment : fulfillment,
                voucherCode,
                version + 1);
    }

    public UcpCheckoutSession withCompleted(String placedOrderId, Instant now) {
        return withCompleted(placedOrderId, now, priceSnapshot);
    }

    public UcpCheckoutSession withCompleted(String placedOrderId, Instant now, Map<String, Long> completedPriceSnapshot) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "completed",
                currency,
                items,
                buyer,
                context,
                placedOrderId,
                createdAt,
                now,
                expiresAt,
                completedPriceSnapshot != null ? completedPriceSnapshot : priceSnapshot,
                payment,
                fulfillment,
                voucherCode,
                version + 1);
    }

    public UcpCheckoutSession withCanceled(Instant now) {
        return withCanceled(now, priceSnapshot);
    }

    public UcpCheckoutSession withCanceled(Instant now, Map<String, Long> canceledPriceSnapshot) {
        return new UcpCheckoutSession(
                id,
                userId,
                cartId,
                "canceled",
                currency,
                items,
                buyer,
                context,
                orderId,
                createdAt,
                now,
                expiresAt,
                canceledPriceSnapshot != null ? canceledPriceSnapshot : priceSnapshot,
                payment,
                fulfillment,
                voucherCode,
                version + 1);
    }
}
