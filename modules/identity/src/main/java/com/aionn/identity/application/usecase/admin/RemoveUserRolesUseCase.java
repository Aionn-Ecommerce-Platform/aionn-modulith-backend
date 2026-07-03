package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.admin.command.RemoveUserRolesCommand;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;
import com.aionn.identity.application.mapper.AdminResultMapper;
import com.aionn.identity.application.port.in.admin.RemoveUserRolesInputPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveUserRolesUseCase implements RemoveUserRolesInputPort {

    private final AdminUserService adminUserService;
    private final AdminResultMapper adminResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRolesResult execute(RemoveUserRolesCommand command) {
        var result = adminUserService.removeRoles(command.userId(), command.roles());
        return adminResultMapper.toUserRolesResult(result);
    }
}

