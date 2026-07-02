package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DataExportRequest {

    private final String requestId;
    private final String userId;
    private DataExportStatus status;
    private final LocalDateTime requestedAt;
    private String fileUrl;
    private LocalDateTime completedAt;

    public DataExportRequest(
            String requestId,
            String userId,
            DataExportStatus status,
            LocalDateTime requestedAt,
            String fileUrl,
            LocalDateTime completedAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.fileUrl = fileUrl;
        this.completedAt = completedAt;
    }

    public static DataExportRequest createRequested(String requestId, String userId) {
        return new DataExportRequest(
                requestId,
                userId,
                DataExportStatus.REQUESTED,
                LocalDateTime.now(),
                null,
                null);
    }
}



