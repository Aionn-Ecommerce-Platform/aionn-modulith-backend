package com.aionn.recommendation.domain.valueobject;

public enum InteractionType {

    VIEW,
    SEARCH,
    WISHLIST_ADD,
    CART_ADD,
    ORDER_PLACED,
    PURCHASE;

    public boolean isStrongSignal() {
        return this == CART_ADD || this == PURCHASE;
    }
}
