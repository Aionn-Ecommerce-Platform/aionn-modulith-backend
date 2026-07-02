package com.aionn.identity.adapter.rest.mapper.auth;

import com.aionn.identity.adapter.rest.dto.auth.request.LinkSocialRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.LoginRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.RefreshTokenRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.SocialAuthRequest;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthSessionResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.SocialAuthResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.SocialLinkResponse;
import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.command.LoginCommand;
import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.command.LogoutCommand;
import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;
import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;
import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;
import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;
import com.aionn.identity.application.dto.auth.query.GetAuthSessionsQuery;
import com.aionn.identity.application.dto.auth.result.AuthSessionResult;
import com.aionn.identity.application.dto.auth.result.LoginResult;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.dto.auth.result.SocialLinkResult;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthDtoMapper {

    @Mapping(target = "identity", source = "request.identity")
    @Mapping(target = "password", source = "request.password")
    @Mapping(target = "mfaCode", source = "request.mfaCode")
    @Mapping(target = "ipAddress", source = "clientIp")
    @Mapping(target = "userAgent", source = "userAgent")
    LoginCommand toLoginCommand(LoginRequest request, String clientIp, String userAgent);

    @Mapping(target = "provider", source = "request.provider")
    @Mapping(target = "providerToken", source = "request.providerToken")
    @Mapping(target = "ipAddress", source = "clientIp")
    @Mapping(target = "userAgent", source = "userAgent")
    SocialLoginCommand toSocialLoginCommand(SocialAuthRequest request, String clientIp, String userAgent);

    @Mapping(target = "requestRefreshToken", source = "request.refreshToken")
    @Mapping(target = "cookieRefreshToken", source = "cookieToken")
    @Mapping(target = "clientIp", source = "clientIp")
    @Mapping(target = "userAgent", source = "userAgent")
    RefreshTokenCommand toRefreshCommand(RefreshTokenRequest request, String cookieToken, String clientIp,
            String userAgent);

    default GetAuthSessionsQuery toGetSessionsQuery(String userId) {
        return new GetAuthSessionsQuery(userId);
    }

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "provider", source = "request.provider")
    @Mapping(target = "providerToken", source = "request.providerToken")
    LinkSocialCommand toLinkSocialCommand(String userId, LinkSocialRequest request);

    default UnlinkSocialCommand toUnlinkSocialCommand(String userId, String provider) {
        return new UnlinkSocialCommand(userId, provider);
    }

    default RevokeSessionCommand toRevokeSessionCommand(String userId, String sessionId) {
        return new RevokeSessionCommand(userId, sessionId);
    }

    default LogoutCommand toLogoutCommand(String userId, String sessionId, String accessTokenJti) {
        return new LogoutCommand(userId, sessionId, accessTokenJti);
    }

    default LogoutAllCommand toLogoutAllCommand(String userId) {
        return new LogoutAllCommand(userId);
    }

    AuthTokenResponse toAuthTokenResponse(LoginResult result);

    AuthTokenResponse toAuthTokenResponse(RefreshAccessTokenResult result);

    AuthTokenResponse toAuthTokenResponse(SocialLoginResult result);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "expiresAt", source = "expiresAt")
    SocialAuthResponse toSocialLoginResponse(SocialLoginResult result);

    SocialLinkResponse toSocialLinkResponse(SocialLinkResult result);

    AuthSessionResponse toAuthSessionResponse(AuthSessionResult result);

    List<AuthSessionResponse> toAuthSessionResponses(List<AuthSessionResult> results);

    LogoutAllResponse toLogoutAllResponse(LogoutAllResult result);
}
