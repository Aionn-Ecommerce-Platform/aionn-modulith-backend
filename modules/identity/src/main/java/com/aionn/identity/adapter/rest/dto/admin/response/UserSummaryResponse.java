package com.aionn.identity.adapter.rest.dto.admin.response;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;

public record UserSummaryResponse(
                String userId,
                String email,
                String displayName,
                UserStatus status,
                UserRole primaryRole) {
}


