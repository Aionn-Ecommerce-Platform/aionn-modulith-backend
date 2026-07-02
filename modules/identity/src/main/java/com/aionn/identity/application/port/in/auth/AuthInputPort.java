package com.aionn.identity.application.port.in.auth;

public interface AuthInputPort extends
        SocialAuthInputPort,
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
