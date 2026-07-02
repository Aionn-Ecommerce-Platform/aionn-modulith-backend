package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.LogoutCommand;
import com.aionn.identity.application.port.in.auth.LogoutInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutUseCase implements LogoutInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(LogoutCommand command) {
        authService.logout(command);
    }
}

