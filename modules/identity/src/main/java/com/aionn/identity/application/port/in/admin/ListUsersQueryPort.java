package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.admin.query.ListUsersQuery;
import com.aionn.identity.application.dto.admin.result.UserListResult;

public interface ListUsersQueryPort {
    UserListResult execute(ListUsersQuery query);
}
