package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
import com.aionn.identity.application.port.in.auth.SocialLoginInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialLoginUseCase implements SocialLoginInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialLoginResult execute(SocialLoginCommand command) {
        return authService.socialLogin(command);
    }
}

