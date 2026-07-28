package com.aionn.chat.application.dto.block.result;

import java.time.Instant;

public record BlockResult(
        String blockId,
        String blockerId,
        String blockedId,
        String reason,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}

