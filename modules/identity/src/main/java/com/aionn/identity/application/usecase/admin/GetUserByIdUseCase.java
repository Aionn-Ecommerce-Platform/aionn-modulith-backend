package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.admin.query.GetUserQuery;
import com.aionn.identity.application.dto.admin.result.UserDetailResult;
import com.aionn.identity.application.port.in.admin.GetUserByIdQueryPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserByIdUseCase implements GetUserByIdQueryPort {

    private final AdminUserService adminUserService;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public UserDetailResult execute(GetUserQuery query) {
        return adminUserService.getUserById(query.userId());
    }
}

