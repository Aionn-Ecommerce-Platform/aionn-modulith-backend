package com.aionn.identity.application.dto.user.view;

import java.time.Instant;

public record DataExportRequestView(
                String requestId,
                String status,
                Instant requestedAt) {
}



