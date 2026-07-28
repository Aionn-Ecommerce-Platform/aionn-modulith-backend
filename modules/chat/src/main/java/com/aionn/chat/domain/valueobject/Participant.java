package com.aionn.chat.domain.valueobject;

import java.time.Instant;
import java.util.Objects;

public record Participant(
        String userId,
        ParticipantRole role,
        String displayName,
        String avatarUrl,
        Instant joinedAt,
        Instant lastReadAt) {

    public Participant {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    public Participant withLastReadAt(Instant readAt) {
        return new Participant(userId, role, displayName, avatarUrl, joinedAt, readAt);
    }
}

