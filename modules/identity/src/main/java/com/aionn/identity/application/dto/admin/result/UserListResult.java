package com.aionn.identity.application.dto.admin.result;

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
                        String status,
                        String primaryRole) {
        }
}


