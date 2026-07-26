package com.aionn.promotion.domain.valueobject;

public enum CampaignStatus {
    DRAFT,
    SCHEDULED,
    RUNNING,
    ENDED,
    CANCELLED;

    public boolean canTransitionTo(CampaignStatus next) {
        return switch (this) {
            case DRAFT -> next == SCHEDULED || next == CANCELLED;
            case SCHEDULED -> next == RUNNING || next == CANCELLED;
            case RUNNING -> next == ENDED || next == CANCELLED;
            case ENDED, CANCELLED -> false;
        };
    }

    public boolean isActive() {
        return this == RUNNING;
    }
}

