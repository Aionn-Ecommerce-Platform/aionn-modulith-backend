package com.aionn.catalog.domain.valueobject;

public enum ProductStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    HIDDEN,
    REJECTED,
    TAKEN_DOWN;

    public boolean canTransitionTo(ProductStatus next) {
        return switch (this) {
            case DRAFT -> next == PENDING_REVIEW || next == PUBLISHED || next == REJECTED || next == HIDDEN;
            case PENDING_REVIEW -> next == PUBLISHED || next == REJECTED || next == DRAFT;
            case PUBLISHED -> next == HIDDEN || next == TAKEN_DOWN;
            case REJECTED -> next == DRAFT;
            case HIDDEN -> next == PUBLISHED || next == DRAFT;
            case TAKEN_DOWN -> false;
        };
    }

    public boolean isSearchable() {
        return this == PUBLISHED;
    }
}
