package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class DataExportRequest {

    private final String requestId;
    private final String userId;
    private DataExportStatus status;
    private final Instant requestedAt;
    private String fileUrl;
    private Instant completedAt;

    public DataExportRequest(
            String requestId,
            String userId,
            DataExportStatus status,
            Instant requestedAt,
            String fileUrl,
            Instant completedAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.fileUrl = fileUrl;
        this.completedAt = completedAt;
    }

    public static DataExportRequest createRequested(String requestId, String userId) {
        return createRequested(requestId, userId, Clock.systemUTC());
    }

    public static DataExportRequest createRequested(String requestId, String userId, Clock clock) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return new DataExportRequest(
                requestId,
                userId,
                DataExportStatus.REQUESTED,
                clock.instant(),
                null,
                null);
    }

    public void markProcessing() {
        transitionTo(DataExportStatus.PROCESSING);
    }

    public void complete(String fileUrl) {
        complete(fileUrl, Clock.systemUTC());
    }

    public void complete(String fileUrl, Clock clock) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl must not be blank");
        }
        transitionTo(DataExportStatus.COMPLETED);
        this.fileUrl = fileUrl;
        this.completedAt = clock.instant();
    }

    public void fail() {
        fail(Clock.systemUTC());
    }

    public void fail(Clock clock) {
        transitionTo(DataExportStatus.FAILED);
        this.completedAt = clock.instant();
    }

    private void transitionTo(DataExportStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Data export cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }
}

