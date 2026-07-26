package com.aionn.promotion.domain.valueobject;

public enum FlashSaleRegistrationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(FlashSaleRegistrationStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED, REJECTED, CANCELLED -> false;
        };
    }
}
