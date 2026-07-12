package com.aionn.identity.application.dto.user.view;

import java.time.Instant;

public record DeletionRequestView(
                String requestId,
                String status,
                Instant requestedAt,
                Instant scheduledDeletionAt) {
}

