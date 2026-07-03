package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.UnlockAccountCommand;
import com.aionn.identity.application.port.in.security.UnlockAccountInputPort;
import com.aionn.identity.application.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnlockAccountUseCase implements UnlockAccountInputPort {

    private final AdminUserService adminUserService;

    @Override
    @Transactional
    public void execute(UnlockAccountCommand command) {
        adminUserService.unlockAccount(command.userId());
    }
}

