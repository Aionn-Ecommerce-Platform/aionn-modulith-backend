package com.aionn.catalog.domain.valueobject;

public enum MerchantStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED;

    public boolean canTransitionTo(MerchantStatus next) {
        return switch (this) {
            case PENDING -> next == ACTIVE || next == CLOSED;
            case ACTIVE -> next == SUSPENDED || next == CLOSED;
            case SUSPENDED -> next == ACTIVE || next == CLOSED;
            case CLOSED -> false;
        };
    }
}
