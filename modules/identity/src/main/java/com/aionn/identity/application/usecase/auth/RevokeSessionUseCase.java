package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;
import com.aionn.identity.application.port.in.auth.RevokeSessionInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeSessionUseCase implements RevokeSessionInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(RevokeSessionCommand command) {
        authService.revokeSession(command);
    }
}

