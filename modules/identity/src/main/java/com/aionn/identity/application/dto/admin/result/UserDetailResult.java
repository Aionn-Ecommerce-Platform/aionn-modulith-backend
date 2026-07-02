package com.aionn.identity.application.dto.admin.result;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDetailResult(
        String userId,
        String email,
        String phone,
        String displayName,
        Set<String> roles,
        String status,
        LocalDateTime createdAt,
        LocalDateTime emailVerifiedAt,
        LocalDateTime phoneVerifiedAt) {
}

