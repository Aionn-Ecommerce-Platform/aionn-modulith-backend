package com.aionn.identity.application.dto.admin.query;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.application.query.Query;

public record ListUsersQuery(UserStatus status, UserRole role, int page, int size) implements Query {
}

