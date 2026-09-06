package com.aionn.recommendation.domain.valueobject;

/**
 * Behavioural signal types the module ingests. Ordered weakest to strongest intent; the numeric
 * weight and decay attached to each one is configuration, not domain knowledge, and arrives through
 * {@link InteractionWeight}.
 */
public enum InteractionType {

    VIEW,
    CART_ADD,
    PURCHASE;

    /**
     * Strong signals express deliberate intent and are the only ones used for collaborative
     * filtering: views are noisy because users browse widely without meaning to buy.
     */
    public boolean isStrongSignal() {
        return this != VIEW;
    }
}
