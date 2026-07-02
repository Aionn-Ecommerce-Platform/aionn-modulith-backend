package com.aionn.identity.domain.valueobject;

public enum DataExportStatus {
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(DataExportStatus newStatus) {
        return switch (this) {
            case REQUESTED -> newStatus == PROCESSING || newStatus == FAILED;
            case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}


