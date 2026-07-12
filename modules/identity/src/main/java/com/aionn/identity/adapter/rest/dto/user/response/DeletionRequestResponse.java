package com.aionn.identity.adapter.rest.dto.user.response;

import java.time.Instant;

public record DeletionRequestResponse(
        String requestId,
        String status,
        Instant requestedAt,
        Instant scheduledDeletionAt) {
}


