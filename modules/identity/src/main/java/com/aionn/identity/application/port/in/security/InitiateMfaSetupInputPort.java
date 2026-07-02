package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.command.InitiateMfaSetupCommand;
import com.aionn.identity.application.dto.security.result.MfaSetupResult;

public interface InitiateMfaSetupInputPort {
    MfaSetupResult execute(InitiateMfaSetupCommand command);
}
