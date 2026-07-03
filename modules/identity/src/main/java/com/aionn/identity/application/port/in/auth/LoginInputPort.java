package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.LoginCommand;
import com.aionn.identity.application.dto.auth.result.LoginResult;

public interface LoginInputPort {

    LoginResult execute(LoginCommand command);
}
