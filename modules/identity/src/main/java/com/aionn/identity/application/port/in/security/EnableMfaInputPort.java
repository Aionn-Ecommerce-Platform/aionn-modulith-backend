package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.command.EnableMfaCommand;
import com.aionn.identity.application.dto.security.result.MfaResult;

public interface EnableMfaInputPort {
    MfaResult execute(EnableMfaCommand command);
}



