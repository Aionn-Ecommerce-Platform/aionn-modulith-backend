package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;

public interface RefreshTokenInputPort {

    RefreshAccessTokenResult execute(RefreshTokenCommand command);
}

