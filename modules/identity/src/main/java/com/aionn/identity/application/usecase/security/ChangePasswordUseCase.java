package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.ChangePasswordCommand;
import com.aionn.identity.application.port.in.security.ChangePasswordInputPort;
import com.aionn.identity.application.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements ChangePasswordInputPort {

    private final PasswordResetService passwordResetService;

    @Override
    @Transactional
    public void execute(ChangePasswordCommand command) {
        passwordResetService.changePassword(command.userId(), command.currentPassword(), command.newPassword(),
                command.clientIp());
    }
}

