package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;

public interface RevokeSessionInputPort {

    void execute(RevokeSessionCommand command);
}



