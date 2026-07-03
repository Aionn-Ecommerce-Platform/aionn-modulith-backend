package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.InitiateMfaSetupCommand;
import com.aionn.identity.application.dto.security.result.MfaSetupResult;
import com.aionn.identity.application.port.in.security.InitiateMfaSetupInputPort;
import com.aionn.identity.application.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitiateMfaSetupUseCase implements InitiateMfaSetupInputPort {

    private final MfaService mfaService;

    @Override
    @Transactional
    public MfaSetupResult execute(InitiateMfaSetupCommand command) {
        return mfaService.initiateSetup(command.userId(), command.password(), command.clientIp());
    }
}
