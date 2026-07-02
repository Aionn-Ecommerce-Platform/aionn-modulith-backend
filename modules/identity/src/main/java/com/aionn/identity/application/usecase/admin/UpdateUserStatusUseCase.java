package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.admin.command.UpdateUserStatusCommand;
import com.aionn.identity.application.dto.admin.result.UserStatusResult;
import com.aionn.identity.application.mapper.AdminResultMapper;
import com.aionn.identity.application.port.in.admin.UpdateUserStatusInputPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserStatusUseCase implements UpdateUserStatusInputPort {

    private final AdminUserService adminUserService;
    private final AdminResultMapper adminResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserStatusResult execute(UpdateUserStatusCommand command) {
        var result = adminUserService.updateStatus(command.userId(), command.status());
        return adminResultMapper.toUserStatusResult(result);
    }
}

