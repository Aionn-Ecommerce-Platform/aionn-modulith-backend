package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.EnableMfaCommand;
import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.port.in.security.EnableMfaInputPort;
import com.aionn.identity.application.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnableMfaUseCase implements EnableMfaInputPort {

    private final MfaService mfaService;

    @Override
    @Transactional
    public MfaResult execute(EnableMfaCommand command) {
        return mfaService.enableMfa(command.userId(), command.password(), command.mfaCode(), command.clientIp());
    }
}
