package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;

public interface SocialLoginInputPort {

    SocialLoginResult socialLogin(SocialLoginCommand command);
}
