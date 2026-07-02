package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;

public interface RefreshAccessTokenInputPort {

    RefreshAccessTokenResult execute(RefreshTokenCommand command);
}
