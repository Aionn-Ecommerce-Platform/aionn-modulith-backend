package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.dto.security.command.DisableMfaCommand;

public interface DisableMfaInputPort {
    MfaResult execute(DisableMfaCommand command);
}



