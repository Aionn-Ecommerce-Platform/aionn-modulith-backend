package com.aionn.identity.adapter.rest.dto.admin.response;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDetailResponse(
                String userId,
                String email,
                String phone,
                String displayName,
                Set<UserRole> roles,
                UserStatus status,
                LocalDateTime createdAt,
                LocalDateTime emailVerifiedAt,
                LocalDateTime phoneVerifiedAt) {
}


