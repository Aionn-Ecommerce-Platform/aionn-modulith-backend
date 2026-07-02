package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;
import com.aionn.identity.application.port.in.auth.UnlinkSocialInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnlinkSocialUseCase implements UnlinkSocialInputPort {

    private final AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(UnlinkSocialCommand command) {
        authService.unlinkSocial(command);
    }
}

