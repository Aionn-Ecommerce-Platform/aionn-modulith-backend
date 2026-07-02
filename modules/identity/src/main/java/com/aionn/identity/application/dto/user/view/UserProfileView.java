package com.aionn.identity.application.dto.user.view;

import java.time.LocalDateTime;
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
                LocalDateTime emailVerifiedAt,
                LocalDateTime phoneVerifiedAt,
                LocalDateTime createdAt) {
}



