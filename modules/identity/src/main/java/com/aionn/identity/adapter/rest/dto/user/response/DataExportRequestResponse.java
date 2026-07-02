package com.aionn.identity.adapter.rest.dto.user.response;

import java.time.LocalDateTime;

public record DataExportRequestResponse(
        String requestId,
        String status,
        LocalDateTime requestedAt) {
}


