package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.dto.security.command.DisableMfaCommand;
import com.aionn.identity.application.port.in.security.DisableMfaInputPort;
import com.aionn.identity.application.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DisableMfaUseCase implements DisableMfaInputPort {

    private final MfaService mfaService;

    @Override
    @Transactional
    public MfaResult execute(DisableMfaCommand command) {
        return mfaService.disableMfa(command.userId(), command.password(), command.mfaCode(), command.clientIp());
    }
}
