package com.aionn.identity.application.usecase.security;

import com.aionn.identity.application.dto.security.command.RegenerateBackupCodesCommand;
import com.aionn.identity.application.dto.security.result.BackupCodesResult;
import com.aionn.identity.application.port.in.security.RegenerateBackupCodesInputPort;
import com.aionn.identity.application.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegenerateBackupCodesUseCase implements RegenerateBackupCodesInputPort {

    private final MfaService mfaService;

    @Override
    @Transactional
    public BackupCodesResult execute(RegenerateBackupCodesCommand command) {
        var rawCodes = mfaService.regenerateBackupCodes(
                command.userId(), command.password(), command.mfaCode(), command.clientIp());
        return new BackupCodesResult(rawCodes);
    }
}
