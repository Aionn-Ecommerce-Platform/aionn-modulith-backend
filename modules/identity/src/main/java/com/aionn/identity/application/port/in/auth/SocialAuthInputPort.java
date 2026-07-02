package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;

public interface SocialAuthInputPort {

    SocialLoginResult execute(SocialLoginCommand command);
}
