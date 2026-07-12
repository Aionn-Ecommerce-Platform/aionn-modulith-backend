package com.aionn.identity.application.dto.admin.result;

import java.time.Instant;
import java.util.Set;

public record UserDetailResult(
        String userId,
        String email,
        String phone,
        String displayName,
        Set<String> roles,
        String status,
        Instant createdAt,
        Instant emailVerifiedAt,
        Instant phoneVerifiedAt) {
}

