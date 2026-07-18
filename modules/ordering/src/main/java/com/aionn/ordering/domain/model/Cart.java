package com.aionn.ordering.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.ordering.domain.event.CartEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Cart extends AggregateRoot {

    private final String cartId;
    private final String userId;
    private final Map<String, Integer> items = new LinkedHashMap<>();
    private String voucherCode;
    private final Instant createdAt;
    private Instant updatedAt;

    public Cart(String cartId, String userId, Map<String, Integer> items, String voucherCode,
            Instant createdAt, Instant updatedAt) {
        this.cartId = cartId;
        this.userId = userId;
        if (items != null) {
            this.items.putAll(items);
        }
        this.voucherCode = voucherCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cart create(String cartId, String userId, Instant now) {
        return new Cart(cartId, userId, Map.of(), null, now, now);
    }

    public void ensureOwnedBy(String userId) {
        Guard.require(this.userId.equals(userId),
                () -> new OrderingException(OrderingErrorCode.CART_FORBIDDEN));
    }

    public void addItem(String skuId, int qty, Instant now) {
        Guard.require(qty > 0,
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "qty must be > 0"));
        items.merge(skuId, qty, Integer::sum);
        touch(now);
        registerEvent(new CartEvents.ItemAddedToCart(cartId, userId, skuId, items.get(skuId), updatedAt));
    }

    public void updateItemQty(String skuId, int newQty, Instant now) {
        Guard.require(items.containsKey(skuId),
                () -> new OrderingException(OrderingErrorCode.CART_ITEM_NOT_FOUND));
        Guard.require(newQty >= 0,
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "qty must be >= 0"));
        if (newQty == 0) {
            items.remove(skuId);
            touch(now);
            registerEvent(new CartEvents.CartItemRemoved(cartId, skuId, updatedAt));
            return;
        }
        items.put(skuId, newQty);
        touch(now);
        registerEvent(new CartEvents.CartItemUpdated(cartId, skuId, newQty, updatedAt));
    }

    public void removeItem(String skuId, Instant now) {
        Guard.require(items.remove(skuId) != null,
                () -> new OrderingException(OrderingErrorCode.CART_ITEM_NOT_FOUND));
        touch(now);
        registerEvent(new CartEvents.CartItemRemoved(cartId, skuId, updatedAt));
    }

    /**
     * Idempotent variant of {@link #removeItem(String)}: silently no-ops when
     * the SKU is not in the cart. Used by post-place-order cleanup where the
     * cart may have been mutated concurrently or the order was placed via a
     * "buy now" flow that bypasses the cart.
     */
    public void removeItemIfPresent(String skuId, Instant now) {
        if (items.remove(skuId) == null) {
            return;
        }
        touch(now);
        registerEvent(new CartEvents.CartItemRemoved(cartId, skuId, updatedAt));
    }

    public void clear(String reason, Instant now) {
        if (items.isEmpty() && voucherCode == null) {
            return;
        }
        items.clear();
        voucherCode = null;
        touch(now);
        registerEvent(new CartEvents.CartCleared(cartId, userId, reason, updatedAt));
    }

    public void applyVoucher(String voucherCode, Instant now) {
        Guard.require(voucherCode != null && !voucherCode.isBlank(),
                () -> new OrderingException(OrderingErrorCode.INVALID_ARGUMENT, "voucherCode must not be blank"));
        this.voucherCode = voucherCode.trim();
        touch(now);
        registerEvent(new CartEvents.VoucherApplied(cartId, this.voucherCode, updatedAt));
    }

    public void removeVoucher(Instant now) {
        if (this.voucherCode == null) {
            return;
        }
        this.voucherCode = null;
        touch(now);
        registerEvent(new CartEvents.VoucherRemoved(cartId, updatedAt));
    }

    public List<Map.Entry<String, Integer>> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(items.entrySet()));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    @Override
    protected String aggregateId() {
        return cartId;
    }
}
