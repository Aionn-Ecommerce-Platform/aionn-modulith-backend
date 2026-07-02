package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.result.BackupCodesResult;
import com.aionn.identity.application.dto.security.command.RegenerateBackupCodesCommand;

public interface RegenerateBackupCodesInputPort {
    BackupCodesResult execute(RegenerateBackupCodesCommand command);
}



