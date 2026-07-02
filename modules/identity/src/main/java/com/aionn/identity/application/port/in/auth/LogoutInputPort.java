package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.LogoutCommand;

public interface LogoutInputPort {

    void execute(LogoutCommand command);
}



