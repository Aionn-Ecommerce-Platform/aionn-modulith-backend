package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.admin.query.ListUsersQuery;
import com.aionn.identity.application.dto.admin.result.UserListResult;
import com.aionn.identity.application.port.in.admin.ListUsersQueryPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase implements ListUsersQueryPort {

    private final AdminUserService adminUserService;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public UserListResult execute(ListUsersQuery query) {
        return adminUserService.listUsers(query.status(), query.role(), query.page(), query.size());
    }
}

