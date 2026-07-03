package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.command.LoginCommand;
import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.command.LogoutCommand;
import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;
import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;
import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;
import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;
import com.aionn.identity.application.dto.auth.result.LoginResult;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
import com.aionn.identity.application.mapper.AuthResultMapper;
import com.aionn.identity.application.policy.AuthPolicy;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.TotpManagerPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.application.port.out.social.SocialLinkPersistencePort;
import com.aionn.identity.application.port.out.social.SocialTokenVerifierPort;
import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserPersistencePort userPersistencePort;
    private final UserSecurityPort userSecurityPort;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final SocialLinkPersistencePort socialLinkPersistencePort;
    private final MfaPersistencePort mfaPersistencePort;
    private final PasswordHasherPort passwordHasher;
    private final TotpManagerPort totpManager;
    private final AccessTokenIssuerPort accessTokenIssuer;
    private final SocialTokenVerifierPort socialTokenVerifier;
    private final AuthPolicy authPolicy;
    private final RefreshTokenStorePort refreshTokenStore;
    private final AuthResultMapper authResultMapper;
    private final TokenBlacklistPort tokenBlacklist;
    private final IdentityMetricsPort identityMetrics;

    public LoginResult login(LoginCommand command) {
        log.debug("Login attempt for identity: {}", command.identity());
        IdentityUser user = validateCredentials(command.identity(), command.password());
        validateMfa(user.getUserId(), command.mfaCode());
        userSecurityPort.resetFailedLoginAttempts(user.getUserId());

        AuthSession session = createSession(user.getUserId(), command.ipAddress(), command.userAgent());
        AuthSession savedSession = authSessionPersistencePort.save(session);
        String accessToken = issueAccessToken(savedSession);
        String refreshToken = issueRefreshToken(savedSession);
        LocalDateTime accessTokenExpiresAt = extractAccessTokenExpiry(accessToken);

        log.info("User logged in: userId={}, sessionId={}", user.getUserId(), savedSession.getSessionId());
        identityMetrics.loginAttempt("success");
        identityMetrics.sessionLifecycle("created");
        return authResultMapper.toLoginResult(savedSession, accessToken, refreshToken, accessTokenExpiresAt);
    }

    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        log.debug("Social login attempt: provider={}", command.provider());

        AuthProvider provider = AuthProvider.from(command.provider());
        SocialUserProfile profile = socialTokenVerifier.verifyAndExtract(provider, command.providerToken());
        String providerUserId = profile.providerUserId();

        IdentityUser user;
        boolean isNewUser = false;
        var socialLink = socialLinkPersistencePort.findByProviderAndProviderUserId(provider, providerUserId);
        if (socialLink.isPresent()) {
            user = userPersistencePort.findById(socialLink.get().userId())
                    .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        } else {
            user = createUserForSocial(provider, profile);
            IdentityUser savedUser = userPersistencePort.save(user);
            SocialLink newSocialLink = SocialLink.createNew(
                    IdGenerator.ulid(),
                    savedUser.getUserId(),
                    provider,
                    providerUserId);
            socialLinkPersistencePort.save(newSocialLink, savedUser.getUserId());
            user = savedUser;
            isNewUser = true;
            log.info("New user via social login: userId={}, provider={}", user.getUserId(), provider);
        }

        validateActiveUser(user);
        AuthSession session = createSession(user.getUserId(), command.ipAddress(), command.userAgent());
        AuthSession savedSession = authSessionPersistencePort.save(session);
        String accessToken = issueAccessToken(savedSession);
        String refreshToken = issueRefreshToken(savedSession);
        LocalDateTime accessTokenExpiresAt = extractAccessTokenExpiry(accessToken);

        identityMetrics.socialAuth(provider.name(), "success");
        identityMetrics.sessionLifecycle("created");
        return authResultMapper.toSocialLoginResult(
                savedSession,
                accessToken,
                refreshToken,
                accessTokenExpiresAt,
                isNewUser);
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listSessions(String userId) {
        validateUserExists(userId);
        return authSessionPersistencePort.findByUserId(userId);
    }

    public SocialLink linkSocial(LinkSocialCommand command) {
        IdentityUser user = validateUserExists(command.userId());
        AuthProvider provider = AuthProvider.from(command.provider());
        String providerUserId = socialTokenVerifier.verifyAndExtract(provider, command.providerToken()).providerUserId();

        if (socialLinkPersistencePort.existsByProviderAndProviderUserId(provider, providerUserId)
                || socialLinkPersistencePort.findByUserIdAndProvider(command.userId(), provider).isPresent()) {
            throw new IdentityException(IdentityErrorCode.SOCIAL_LINK_EXISTS);
        }

        SocialLink domainSocialLink = SocialLink.createNew(
                IdGenerator.ulid(),
                user.getUserId(),
                provider,
                providerUserId);
        return socialLinkPersistencePort.save(domainSocialLink, user.getUserId());
    }

    public void unlinkSocial(UnlinkSocialCommand command) {
        IdentityUser user = validateUserExists(command.userId());
        AuthProvider provider = AuthProvider.from(command.provider());
        socialLinkPersistencePort.findByUserIdAndProvider(command.userId(), provider)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.SOCIAL_LINK_NOT_FOUND));
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean hasOtherCredential = hasPassword
                || (user.getPhone() != null && !user.getPhone().isBlank())
                || (user.getEmail() != null && !user.getEmail().isBlank());
        if (!hasOtherCredential) {
            throw new IdentityException(IdentityErrorCode.SOCIAL_LINK_NOT_FOUND,
                    "Cannot unlink the only login method on this account");
        }

        socialLinkPersistencePort.deleteByUserIdAndProvider(command.userId(), provider);
    }

    public void revokeSession(RevokeSessionCommand command) {
        revokeSessionInternal(command.userId(), command.sessionId());
    }

    public void logout(LogoutCommand command) {
        revokeSessionInternal(command.userId(), command.sessionId());
        if (command.accessTokenJti() != null && !command.accessTokenJti().isBlank()) {
            tokenBlacklist.blacklist(
                    command.accessTokenJti(),
                    Duration.ofMinutes(authPolicy.getAccessTokenExpiryMinutes()));
        }
    }

    public LogoutAllResult logoutAll(LogoutAllCommand command) {
        requireUserExists(command.userId());
        int revoked = 0;
        var sessions = authSessionPersistencePort.findByUserId(command.userId());
        for (AuthSession session : sessions) {
            if (AuthSessionStatus.ACTIVE.equals(session.getStatus())) {
                session.revoke();
                refreshTokenStore.revokeBySessionId(session.getSessionId());
                revoked++;
            }
        }
        authSessionPersistencePort.saveAll(sessions);
        return authResultMapper.toLogoutAllResult(revoked);
    }

    public RefreshAccessTokenResult refreshToken(RefreshTokenCommand command) {
        String tokenId = pickToken(command);
        if (tokenId == null) {
            throw new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID);
        }

        // Atomic find-and-revoke: prevents two concurrent refresh calls from both
        // succeeding with the same token and lets a replay attempt be detected later.
        String sessionId = refreshTokenStore.consumeSessionId(tokenId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID));

        AuthSession session = authSessionPersistencePort.findById(sessionId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.SESSION_NOT_FOUND));
        if (!AuthSessionStatus.ACTIVE.equals(session.getStatus()) || session.isExpired()) {
            // Token already consumed above; revoke the whole session so any sibling tokens
            // issued from the same family are invalidated as well.
            refreshTokenStore.revokeBySessionId(sessionId);
            throw new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID);
        }

        session.extendExpiry(nowUtc().plusDays(authPolicy.getSessionExpiresDays()));
        AuthSession refreshed = authSessionPersistencePort.save(session);

        String accessToken = issueAccessToken(refreshed);
        String newRefreshToken = issueRefreshToken(refreshed);
        LocalDateTime accessTokenExpiresAt = extractAccessTokenExpiry(accessToken);
        return authResultMapper.toRefreshResult(refreshed, accessToken, newRefreshToken, accessTokenExpiresAt);
    }

    private static String pickToken(RefreshTokenCommand command) {
        if (command.requestRefreshToken() != null && !command.requestRefreshToken().isBlank()) {
            return command.requestRefreshToken();
        }
        if (command.cookieRefreshToken() != null && !command.cookieRefreshToken().isBlank()) {
            return command.cookieRefreshToken();
        }
        return null;
    }

    private void revokeSessionInternal(String userId, String sessionId) {
        requireUserExists(userId);
        AuthSession session = authSessionPersistencePort.findById(sessionId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new IdentityException(IdentityErrorCode.SESSION_FORBIDDEN);
        }

        if (AuthSessionStatus.ACTIVE.equals(session.getStatus())) {
            session.revoke();
            authSessionPersistencePort.save(session);
            refreshTokenStore.revokeBySessionId(sessionId);
            identityMetrics.sessionLifecycle("revoked");
        }
    }

    private IdentityUser validateCredentials(String identity, String password) {
        var userSecurity = userSecurityPort.findByIdentity(identity)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS));

        if (!userSecurity.status().equals(UserStatus.ACTIVE)) {
            throw new IdentityException(IdentityErrorCode.USER_INACTIVE);
        }
        if (isLocked(userSecurity.lockedUntil())) {
            log.warn("Login attempt against locked account: {}", identity);
            throw new IdentityException(IdentityErrorCode.USER_INACTIVE,
                    "Account is temporarily locked");
        }
        if (userSecurity.passwordHash() == null || !passwordHasher.matches(password, userSecurity.passwordHash())) {
            log.warn("Invalid password attempt for identity: {}", identity);
            recordFailedLoginAttempt(userSecurity);
            identityMetrics.loginAttempt("failed");
            throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        return userPersistencePort.findById(userSecurity.userId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private void validateMfa(String userId, String mfaCode) {
        var userSecurity = userSecurityPort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        if (!userSecurity.mfaEnabled()) {
            return;
        }
        if (mfaCode == null || mfaCode.isBlank()) {
            recordFailedLoginAttempt(userSecurity);
            throw new IdentityException(IdentityErrorCode.OTP_REQUIRED, "MFA code is required");
        }

        boolean matched = false;
        if (userSecurity.mfaSecret() != null && !userSecurity.mfaSecret().isBlank()) {
            matched = mfaPersistencePort.findActiveBackupCodes(userId).stream()
                    .filter(code -> passwordHasher.matches(mfaCode, code.codeHash()))
                    .findFirst()
                    .map(code -> mfaPersistencePort.markBackupCodeUsed(code.backupCodeId(), nowUtc()))
                    .orElse(false);
            if (!matched) {
                matched = totpManager.verifyCode(userSecurity.mfaSecret(), mfaCode);
            }
        }

        if (!matched) {
            recordFailedLoginAttempt(userSecurity);
            throw new IdentityException(IdentityErrorCode.OTP_INVALID, "Invalid MFA code");
        }
    }

    private void recordFailedLoginAttempt(UserSecurityPort.UserSecurityData user) {
        int failedAttempts = user.failedLoginAttempts() + 1;
        LocalDateTime lockedUntil = null;
        if (failedAttempts >= authPolicy.getMaxFailedLoginAttempts()) {
            lockedUntil = nowUtc().plusMinutes(authPolicy.getLockoutMinutes());
        }
        userSecurityPort.recordFailedLoginAttempt(user.userId(), failedAttempts, lockedUntil);
    }

    private boolean isLocked(LocalDateTime lockedUntil) {
        return lockedUntil != null && lockedUntil.isAfter(nowUtc());
    }

    private IdentityUser validateUserExists(String userId) {
        IdentityUser user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        validateActiveUser(user);
        return user;
    }

    private void requireUserExists(String userId) {
        if (!userPersistencePort.existsById(userId)) {
            throw new IdentityException(IdentityErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateActiveUser(IdentityUser user) {
        if (!user.isActive()) {
            throw new IdentityException(IdentityErrorCode.USER_INACTIVE);
        }
    }

    private IdentityUser createUserForSocial(AuthProvider provider, SocialUserProfile profile) {
        String email = (profile.email() != null && !profile.email().isBlank()) ? profile.email().trim() : null;
        if (email != null && userPersistencePort.findByIdentity(email).isPresent()) {
            email = null;
        }
        String username = generateSocialUsername(provider, profile);
        IdentityUser user = IdentityUser.createNew(IdGenerator.ulid(), email, null, username);
        if (email != null) {
            user.verifyEmail();
        }
        if (profile.displayName() != null && !profile.displayName().isBlank()) {
            user.updateDisplayName(profile.displayName());
        }
        return user;
    }

    private String generateSocialUsername(AuthProvider provider, SocialUserProfile profile) {
        String base = sanitizeUsernameBase(profile.email(), profile.displayName());
        if (base == null) {
            base = (provider.name().toLowerCase() + "_" + profile.providerUserId().replace(':', '_')
                    .replaceAll("[^a-zA-Z0-9_]", "")).toLowerCase();
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int suffix = 1;
        while (userPersistencePort.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
            if (candidate.length() > 50) {
                candidate = candidate.substring(0, 50);
            }
        }
        return candidate;
    }

    private String sanitizeUsernameBase(String email, String displayName) {
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@')).toLowerCase()
                    .replaceAll("[^a-z0-9._-]", "");
            if (local.length() >= 3) {
                return local;
            }
        }
        if (displayName != null && !displayName.isBlank()) {
            String slug = java.text.Normalizer.normalize(displayName, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toLowerCase()
                    .replaceAll("\\s+", "_")
                    .replaceAll("[^a-z0-9_]", "");
            if (slug.length() >= 3) {
                return slug;
            }
        }
        return null;
    }

    private AuthSession createSession(String userId, String ipAddress, String userAgent) {
        return AuthSession.createNew(
                IdGenerator.ulid(),
                userId,
                ipAddress,
                userAgent,
                nowUtc().plusDays(authPolicy.getSessionExpiresDays()));
    }

    private String issueAccessToken(AuthSession session) {
        var user = userPersistencePort.findById(session.getUserId()).orElse(null);
        var roles = user != null
                ? user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                : java.util.Set.<String>of();
        return accessTokenIssuer.issueAccessToken(
                session.getUserId(),
                session.getSessionId(),
                toInstant(session.getExpiresAt()),
                roles);
    }

    private LocalDateTime extractAccessTokenExpiry(String accessToken) {
        return accessTokenIssuer.extractExpiry(accessToken)
                .map(AuthService::toLocalDateTime)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID));
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String issueRefreshToken(AuthSession session) {
        byte[] bytes = new byte[com.aionn.identity.application.policy.IdentityValidationConstants.REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Duration ttl = Duration.between(toInstant(nowUtc()), toInstant(session.getExpiresAt()));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofDays(authPolicy.getSessionExpiresDays());
        }
        refreshTokenStore.store(tokenId, session.getSessionId(), ttl);
        return tokenId;
    }
}
