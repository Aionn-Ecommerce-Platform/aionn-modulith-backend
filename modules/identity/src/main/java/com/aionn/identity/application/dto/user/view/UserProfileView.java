package com.aionn.identity.application.dto.user.view;

import java.time.Instant;
import java.util.Set;

public record UserProfileView(
                String userId,
                String email,
                String phone,
                String username,
                String displayName,
                String avatarUrl,
                Set<String> roles,
                String status,
                Instant emailVerifiedAt,
                Instant phoneVerifiedAt,
                Instant createdAt) {
}



