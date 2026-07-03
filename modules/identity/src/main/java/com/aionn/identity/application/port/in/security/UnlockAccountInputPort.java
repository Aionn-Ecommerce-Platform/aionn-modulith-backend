package com.aionn.identity.application.port.in.security;

import com.aionn.identity.application.dto.security.command.UnlockAccountCommand;

public interface UnlockAccountInputPort {
    void execute(UnlockAccountCommand command);
}
