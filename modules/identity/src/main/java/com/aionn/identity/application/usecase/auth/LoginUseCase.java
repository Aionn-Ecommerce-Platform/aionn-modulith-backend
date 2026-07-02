package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.LoginCommand;
import com.aionn.identity.application.dto.auth.result.LoginResult;
import com.aionn.identity.application.port.in.auth.LoginInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase implements LoginInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResult execute(LoginCommand command) {
        return authService.login(command);
    }
}

