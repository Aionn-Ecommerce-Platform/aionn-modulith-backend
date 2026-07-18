package com.aionn.ordering.domain.model;

import com.aionn.sharedkernel.domain.vo.Money;

import java.util.Objects;

public record OrderItem(
        String skuId,
        int qty,
        Money unitPrice,
        String warehouseId,
        String reservationId) {

    public OrderItem {
        Objects.requireNonNull(skuId, "skuId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }
    }

    public Money lineTotal() {
        return unitPrice.multiply(qty);
    }
}
