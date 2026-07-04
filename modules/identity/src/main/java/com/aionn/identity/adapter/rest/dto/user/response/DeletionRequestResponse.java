package com.aionn.identity.adapter.rest.dto.user.response;

import java.time.LocalDateTime;

public record DeletionRequestResponse(
        String requestId,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime scheduledDeletionAt) {
}


