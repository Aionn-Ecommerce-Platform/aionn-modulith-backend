package com.aionn.identity.application.dto.user.view;

import java.time.LocalDateTime;

public record DeletionRequestView(
                String requestId,
                String status,
                LocalDateTime requestedAt,
                LocalDateTime scheduledDeletionAt) {
}

