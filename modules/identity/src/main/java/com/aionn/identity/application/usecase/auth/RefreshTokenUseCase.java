package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.port.in.auth.RefreshTokenInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase implements RefreshTokenInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshAccessTokenResult execute(RefreshTokenCommand command) {
        return authService.refreshToken(command);
    }
}

