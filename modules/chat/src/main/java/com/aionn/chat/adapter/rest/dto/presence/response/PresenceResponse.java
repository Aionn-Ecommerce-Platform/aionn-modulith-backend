package com.aionn.chat.adapter.rest.dto.presence.response;

import java.time.Instant;

public record PresenceResponse(
        String userId,
        boolean online,
        Instant lastSeenAt) {
}
