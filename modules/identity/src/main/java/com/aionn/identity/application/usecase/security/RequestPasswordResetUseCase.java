package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.RequestPasswordResetCommand;
import com.aionn.identity.application.dto.security.result.PasswordResetResult;
import com.aionn.identity.application.port.in.security.RequestPasswordResetInputPort;
import com.aionn.identity.application.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase implements RequestPasswordResetInputPort {

    private final PasswordResetService passwordResetService;

    @Override
    @Transactional
    public PasswordResetResult execute(RequestPasswordResetCommand command) {
        passwordResetService.requestPasswordReset(command.identity(), command.clientIp());
        return PasswordResetResult.acceptedResult();
    }
}

