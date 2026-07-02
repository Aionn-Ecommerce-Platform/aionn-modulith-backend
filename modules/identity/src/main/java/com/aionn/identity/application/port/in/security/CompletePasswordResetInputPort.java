package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.command.CompletePasswordResetCommand;

public interface CompletePasswordResetInputPort {
    void execute(CompletePasswordResetCommand command);
}
