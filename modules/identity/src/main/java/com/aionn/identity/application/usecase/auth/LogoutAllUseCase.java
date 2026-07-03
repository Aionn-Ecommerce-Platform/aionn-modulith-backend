package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.port.in.auth.LogoutAllInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutAllUseCase implements LogoutAllInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogoutAllResult execute(LogoutAllCommand command) {
        return authService.logoutAll(command);
    }
}

