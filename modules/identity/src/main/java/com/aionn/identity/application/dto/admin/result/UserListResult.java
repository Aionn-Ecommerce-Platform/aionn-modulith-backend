package com.aionn.identity.application.dto.admin.result;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import java.util.List;

public record UserListResult(
                List<UserSummary> users,
                int page,
                int size,
                long total) {

        public record UserSummary(
                        String userId,
                        String email,
                        String displayName,
                        UserStatus status,
                        List<UserRole> roles) {
        }
}


