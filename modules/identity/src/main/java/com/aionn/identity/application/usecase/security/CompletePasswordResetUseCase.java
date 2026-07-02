package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.CompletePasswordResetCommand;
import com.aionn.identity.application.port.in.security.CompletePasswordResetInputPort;
import com.aionn.identity.application.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompletePasswordResetUseCase implements CompletePasswordResetInputPort {

    private final PasswordResetService passwordResetService;

    @Override
    @Transactional
    public void execute(CompletePasswordResetCommand command) {
        passwordResetService.completePasswordReset(command.token(), command.newPassword(), command.clientIp());
    }
}

