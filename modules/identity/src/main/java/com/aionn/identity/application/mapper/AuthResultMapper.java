package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.auth.result.AuthSessionResult;
import com.aionn.identity.application.dto.auth.result.LoginResult;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.dto.auth.result.SocialLinkResult;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.SocialLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthResultMapper {

    @Mapping(target = "userId", source = "session.userId")
    @Mapping(target = "sessionId", source = "session.sessionId")
    @Mapping(target = "sessionExpiresAt", source = "session.expiresAt")
    LoginResult toLoginResult(AuthSession session, String accessToken, String refreshToken, LocalDateTime expiresAt);

    @Mapping(target = "userId", source = "session.userId")
    @Mapping(target = "sessionId", source = "session.sessionId")
    @Mapping(target = "sessionExpiresAt", source = "session.expiresAt")
    SocialLoginResult toSocialLoginResult(
            AuthSession session,
            String accessToken,
            String refreshToken,
            LocalDateTime expiresAt,
            boolean newUser);

    @Mapping(target = "userId", source = "session.userId")
    @Mapping(target = "sessionId", source = "session.sessionId")
    @Mapping(target = "sessionExpiresAt", source = "session.expiresAt")
    RefreshAccessTokenResult toRefreshResult(
            AuthSession session,
            String accessToken,
            String refreshToken,
            LocalDateTime expiresAt);

    default LogoutAllResult toLogoutAllResult(int revokedCount) {
        return new LogoutAllResult(revokedCount);
    }

    default AuthSessionResult toAuthSessionResult(AuthSession session) {
        return new AuthSessionResult(
                session.getSessionId(),
                session.getUserId(),
                session.getStatus().name(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getCreatedAt(),
                session.getLastActiveAt(),
                session.getExpiresAt());
    }

    List<AuthSessionResult> toAuthSessionResults(List<AuthSession> sessions);

    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "providerUserId", source = "providerUserId")
    @Mapping(target = "linkedAt", source = "createdAt")
    SocialLinkResult toSocialLinkResult(SocialLink socialLink);
}
