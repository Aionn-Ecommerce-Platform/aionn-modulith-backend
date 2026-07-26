package com.aionn.promotion.domain.valueobject;

public enum UserVoucherStatus {
    CLAIMED,
    RESERVED,
    APPLIED,
    RELEASED,
    EXPIRED;

    public boolean canTransitionTo(UserVoucherStatus next) {
        return switch (this) {
            case CLAIMED -> next == RESERVED || next == EXPIRED || next == RELEASED;
            case RESERVED -> next == APPLIED || next == RELEASED || next == EXPIRED;
            case RELEASED -> next == RESERVED || next == EXPIRED; // can re-reserve after release if still valid
            case APPLIED, EXPIRED -> false;
        };
    }
}

