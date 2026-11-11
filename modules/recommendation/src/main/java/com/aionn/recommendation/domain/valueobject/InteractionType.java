package com.aionn.recommendation.domain.valueobject;

/** Behavioural signal types, weakest to strongest intent. Weights and decay come from {@link InteractionWeight}. */
public enum InteractionType {

    VIEW,
    SEARCH,
    CART_ADD,
    PURCHASE;

    /** Only deliberate-intent signals feed collaborative filtering; views and searches are noisy. */
    public boolean isStrongSignal() {
        return this == CART_ADD || this == PURCHASE;
    }
}
