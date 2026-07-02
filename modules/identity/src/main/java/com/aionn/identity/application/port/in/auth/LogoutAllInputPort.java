package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;

public interface LogoutAllInputPort {

    LogoutAllResult execute(LogoutAllCommand command);
}



