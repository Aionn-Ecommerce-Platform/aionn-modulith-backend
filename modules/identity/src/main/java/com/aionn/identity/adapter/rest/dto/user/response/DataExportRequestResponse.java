package com.aionn.identity.adapter.rest.dto.user.response;

import java.time.Instant;

public record DataExportRequestResponse(
        String requestId,
        String status,
        Instant requestedAt) {
}


