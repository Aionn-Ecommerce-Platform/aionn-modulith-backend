package com.aionn.identity.application.dto.user.view;

import java.time.Instant;

public record DataExportPayloadView(
        String requestId,
        String contentType,
        String payload,
        Instant completedAt) {
}
