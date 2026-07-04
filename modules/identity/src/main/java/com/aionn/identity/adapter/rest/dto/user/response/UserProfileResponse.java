package com.aionn.identity.adapter.rest.dto.user.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserProfileResponse(
        String userId,
        String email,
        String phone,
        String username,
        String displayName,
        String avatarUrl,
        Set<String> roles,
        String status,
        LocalDateTime emailVerifiedAt,
        LocalDateTime phoneVerifiedAt,
        LocalDateTime createdAt) {
}


