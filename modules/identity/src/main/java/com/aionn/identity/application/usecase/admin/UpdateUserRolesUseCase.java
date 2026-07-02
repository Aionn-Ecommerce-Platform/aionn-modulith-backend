package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.admin.command.UpdateUserRolesCommand;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;
import com.aionn.identity.application.mapper.AdminResultMapper;
import com.aionn.identity.application.port.in.admin.UpdateUserRolesInputPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserRolesUseCase implements UpdateUserRolesInputPort {

    private final AdminUserService adminUserService;
    private final AdminResultMapper adminResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRolesResult execute(UpdateUserRolesCommand command) {
        var result = adminUserService.updateRoles(command.userId(), command.roles());
        return adminResultMapper.toUserRolesResult(result);
    }
}

