package com.aionn.chat.adapter.rest.dto.block.response;

import java.time.Instant;

public record BlockResponse(
        String blockId,
        String blockerId,
        String blockedId,
        String reason,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
