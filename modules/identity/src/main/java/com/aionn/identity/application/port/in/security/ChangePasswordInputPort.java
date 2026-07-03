package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.command.ChangePasswordCommand;

public interface ChangePasswordInputPort {
    void execute(ChangePasswordCommand command);
}
