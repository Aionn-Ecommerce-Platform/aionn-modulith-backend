package com.aionn.identity.application.port.in.auth;

public interface AuthInputPort extends
        RefreshTokenInputPort,
        LogoutInputPort,
        SocialLoginInputPort,
        LoginInputPort,
        LinkSocialInputPort,
        GetAuthSessionsQueryPort,
        UnlinkSocialInputPort,
        RevokeSessionInputPort,
        LogoutAllInputPort {
}
