package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.admin.query.GetUserQuery;
import com.aionn.identity.application.dto.admin.result.UserDetailResult;

public interface GetUserByIdQueryPort {
    UserDetailResult execute(GetUserQuery query);
}

