package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.result.PasswordResetResult;
import com.aionn.identity.application.dto.security.command.RequestPasswordResetCommand;

public interface RequestPasswordResetInputPort {
    PasswordResetResult execute(RequestPasswordResetCommand command);
}
