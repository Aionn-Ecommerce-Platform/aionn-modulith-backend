package com.aionn.identity.adapter.rest.dto.admin.response;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import java.util.List;

public record UserSummaryResponse(
                String userId,
                String email,
                String displayName,
                UserStatus status,
                List<UserRole> roles) {
}


